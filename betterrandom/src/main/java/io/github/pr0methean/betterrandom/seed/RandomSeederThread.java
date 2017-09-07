package io.github.pr0methean.betterrandom.seed;

import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import io.github.pr0methean.betterrandom.EntropyCountingRandom;
import io.github.pr0methean.betterrandom.util.LogPreFormatter;
import io.github.pr0methean.betterrandom.util.LooperThread;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

@SuppressWarnings("ClassExplicitlyExtendsThread")
public final class RandomSeederThread extends LooperThread {

  private static final ExecutorService WAKER_UPPER = Executors.newSingleThreadExecutor();
  private static final LogPreFormatter LOG = new LogPreFormatter(RandomSeederThread.class);
  private static final Map<SeedGenerator, RandomSeederThread> INSTANCES =
      Collections.synchronizedMap(new WeakHashMap<>());
  /**
   * Used to avoid full spin-locking when every {@link Random} to be reseeded is an {@link
   * EntropyCountingRandom} and none has spent its entropy.
   */
  private static final long POLL_INTERVAL_MS = 1000;
  private static final long serialVersionUID = 5229976461051217528L;
  private final SeedGenerator seedGenerator;
  private final byte[] seedArray = new byte[8];
  // WeakHashMap-based Set can't be serialized, so read & write this copy instead
  private final Set<Random> prngsSerial = new HashSet<>();
  private transient Set<Random> prngs;
  private transient ByteBuffer seedBuffer;
  private transient Condition waitWhileEmpty;
  private transient Condition waitForEntropyDrain;
  private transient Set<Random> prngsThisIteration;

  /**
   * Private constructor because only one instance per seed source.
   */
  private RandomSeederThread(SeedGenerator seedGenerator) {
    this.seedGenerator = seedGenerator;
    initTransientFields();
  }

  /**
   * Obtain the instance for the given {@link SeedGenerator}, creating and starting it if it doesn't
   * exist.
   */
  public static RandomSeederThread getInstance(SeedGenerator seedGenerator) {
    Objects.requireNonNull(seedGenerator,
        "Trying to get RandomSeederThread for null SeedGenerator");
    return INSTANCES.computeIfAbsent(seedGenerator,
        seedGen -> {
          LOG.info("Creating a RandomSeederThread for %s", seedGen);
          RandomSeederThread thread = new RandomSeederThread(seedGen);
          thread.setName("RandomSeederThread for " + seedGen);
          thread.setDaemon(true);
          thread.start();
          return thread;
        });
  }

  @EnsuresNonNull(
    {"prngs", "seedBuffer", "waitWhileEmpty", "waitForEntropyDrain", "prngsThisIteration"})
  @RequiresNonNull("lock")
  private void initTransientFields(@UnderInitialization RandomSeederThread this) {
    prngs = Collections.synchronizedSet(
        Collections.newSetFromMap(new WeakHashMap<>()));
    seedBuffer = ByteBuffer.wrap(seedArray);
    waitWhileEmpty = lock.newCondition();
    waitForEntropyDrain = lock.newCondition();
    prngsThisIteration = new HashSet<>();
  }

  @Override
  protected Object readResolve() {
    return getInstance(seedGenerator);
  }

  private void readObject(@UnderInitialization RandomSeederThread this, ObjectInputStream in)
      throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    assert lock != null : "@AssumeAssertion(nullness)"; // WTF Checker Framework?!
    initTransientFields();
    if (!prngsSerial.isEmpty()) {
      synchronized (prngs) {
        prngs.addAll(prngsSerial);
      }
      lock.lock();
      try {
        waitWhileEmpty.signalAll();
      } finally {
        lock.unlock();
      }
      prngsSerial.clear();
    }
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    synchronized (prngs) {
      prngsSerial.addAll(prngs);
    }
    out.defaultWriteObject();
    prngsSerial.clear();
  }

  /**
   * Asynchronously triggers reseeding of the given {@link EntropyCountingRandom} if it is
   * associated with a live RandomSeederThread.
   *
   * @return Whether or not the reseed was successfully scheduled.
   */
  public boolean asyncReseed(Random random) {
    if (!(random instanceof EntropyCountingRandom)) {
      // Reseed of non-entropy-counting Random happens every iteration anyway
      return prngs.contains(random);
    }
    boolean eligible;
    synchronized (prngs) {
      eligible = prngs.contains(random);
    }
    if (eligible) {
      WAKER_UPPER.submit(() -> {
        lock.lock();
        try {
          waitForEntropyDrain.signalAll();
        } finally {
          lock.unlock();
        }
      });
      return true;
    } else {
      return false;
    }
  }

  @SuppressWarnings("InfiniteLoopStatement")
  @Override
  public void iterate() throws InterruptedException {
    boolean entropyConsumed = false;
    while (true) {
      synchronized (prngs) {
        prngsThisIteration.addAll(prngs);
      }
      if (prngsThisIteration.isEmpty()) {
        waitWhileEmpty.await();
      } else {
        break;
      }
    }
    Iterator<Random> iterator = prngsThisIteration.iterator();
    for (Random random : iterator) {
      iterator.remove();
      if (random instanceof EntropyCountingRandom
          && ((EntropyCountingRandom) random).entropyBits() > 0) {
        continue;
      } else {
        entropyConsumed = true;
      }
      try {
        if (random instanceof ByteArrayReseedableRandom) {
          ByteArrayReseedableRandom reseedable = (ByteArrayReseedableRandom) random;
          reseedable.setSeed(seedGenerator.generateSeed(reseedable.getNewSeedLength()));
        } else {
          System.arraycopy(seedGenerator.generateSeed(8), 0, seedArray, 0, 8);
          random.setSeed(seedBuffer.getLong(0));
        }
      } catch (SeedException e) {
        LOG.error("%s gave SeedException %s", seedGenerator, e);
        interrupt();
      }
    }
    if (!entropyConsumed) {
      waitForEntropyDrain.await(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }
  }

  public boolean isEmpty() {
    synchronized (prngs) {
      return prngs.isEmpty();
    }
  }

  /**
   * Add one or more {@link Random} instances. The caller must not hold locks on any of these
   * instances that are also acquired during {@link Random#setSeed(long)} or {@link
   * ByteArrayReseedableRandom#setSeed(byte[])}, as one of those methods may be called immediately
   * and this would cause a circular deadlock.
   */
  public void add(Random... randoms) {
    if (isInterrupted()) {
      throw new IllegalStateException("Already shut down");
    }
    Collections.addAll(prngs, randoms);
    lock.lock();
    try {
      waitForEntropyDrain.signalAll();
      waitWhileEmpty.signalAll();
    } finally {
      lock.unlock();
    }
  }

  public void remove(Random... randoms) {
    prngs.removeAll(Arrays.asList(randoms));
  }

  public void stopIfEmpty() {
    if (isEmpty()) {
      interrupt();
    }
  }
}
