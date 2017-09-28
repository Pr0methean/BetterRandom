package io.github.pr0methean.betterrandom.seed;

import static org.checkerframework.checker.nullness.NullnessUtil.castNonNull;

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
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/**
 * Thread that loops over {@link Random} instances and reseeds them.
 *
 * @author Chris Hennick
 * @version $Id: $Id
 */
@SuppressWarnings("ClassExplicitlyExtendsThread")
public final class RandomSeederThread extends LooperThread {

  private static final ExecutorService WAKER_UPPER = Executors.newSingleThreadExecutor();
  private static final LogPreFormatter LOG = new LogPreFormatter(RandomSeederThread.class);
  @SuppressWarnings("StaticCollection")
  private static final @GuardedBy("<self>") Map<SeedGenerator, RandomSeederThread> INSTANCES =
      Collections.synchronizedMap(new WeakHashMap<>(1));
  private static final long serialVersionUID = 5229976461051217528L;
  private final SeedGenerator seedGenerator;
  private final byte[] longSeedArray = new byte[8];
  // WeakHashMap-based Set can't be serialized, so read & write this copy instead
  private final Set<Random> prngsSerial = new HashSet<>();
  private transient @GuardedBy("<self>") Set<Random> prngs;
  private transient ByteBuffer longSeedBuffer;
  private transient @GuardedBy("lock") Condition waitWhileEmpty;
  private transient @GuardedBy("lock") Condition waitForEntropyDrain;
  private transient Set<Random> prngsThisIteration;
  private transient WeakHashMap<ByteArrayReseedableRandom, byte[]> seedArrays;

  public RandomSeederThread(
      ThreadGroup group, Runnable target, String name,
      long stackSize, SeedGenerator seedGenerator) {
    super(group, target, name, stackSize);
    this.seedGenerator = seedGenerator;
    initTransientFields();
  }

  /**
   * Private constructor because only one instance per seed source.
   */
  private RandomSeederThread(final SeedGenerator seedGenerator) {
    this.seedGenerator = seedGenerator;
    initTransientFields();
  }

  /**
   * Obtain the instance for the given {@link SeedGenerator}, creating and starting it if it doesn't
   * exist.
   *
   * @param seedGenerator the {@link SeedGenerator} to use to seed PRNGs registered with this
   *     RandomSeederThread.
   * @return a RandomSeederThread that is running and is backed by {@code seedGenerator}.
   */
  public static RandomSeederThread getInstance(final SeedGenerator seedGenerator) {
    synchronized (INSTANCES) {
      return INSTANCES.computeIfAbsent(seedGenerator,
          seedGen -> {
            LOG.info("Creating a RandomSeederThread for %s", seedGen);
            final RandomSeederThread thread = new RandomSeederThread(seedGen);
            thread.setName("RandomSeederThread for " + seedGen);
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
            return thread;
          });
    }
  }

  @EnsuresNonNull(
      {"prngs", "longSeedBuffer", "longSeedArray", "seedArrays", "waitWhileEmpty",
          "waitForEntropyDrain", "prngsThisIteration"})
  @RequiresNonNull("lock")
  private void initTransientFields(@UnderInitialization RandomSeederThread this) {
    prngs = Collections.synchronizedSet(
        Collections.newSetFromMap(new WeakHashMap<>(1)));
    longSeedBuffer = ByteBuffer.wrap(longSeedArray);
    waitWhileEmpty = lock.newCondition();
    waitForEntropyDrain = lock.newCondition();
    prngsThisIteration = new HashSet<>(1);
    seedArrays = new WeakHashMap<>(1);
  }

  @Override
  protected Object readResolve() {
    return getInstance(seedGenerator);
  }

  private void readObject(@UnderInitialization RandomSeederThread this, final ObjectInputStream in)
      throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    initTransientFields();
    if (!prngsSerial.isEmpty()) {
      synchronized (prngs) {
        prngs.addAll(prngsSerial);
      }
      castNonNull(lock).lock();
      try {
        waitWhileEmpty.signalAll();
      } finally {
        castNonNull(lock).unlock();
      }
      prngsSerial.clear();
    }
  }

  private void writeObject(final ObjectOutputStream out) throws IOException {
    synchronized (prngs) {
      prngsSerial.addAll(prngs);
    }
    out.defaultWriteObject();
    prngsSerial.clear();
  }

  /**
   * Asynchronously triggers reseeding of the given {@link io.github.pr0methean.betterrandom.EntropyCountingRandom}
   * if it is associated with a live RandomSeederThread.
   *
   * @param random a {@link java.util.Random} object.
   * @return Whether or not the reseed was successfully scheduled.
   */
  public boolean asyncReseed(final Random random) {
    if (!(random instanceof EntropyCountingRandom)) {
      // Reseed of non-entropy-counting Random happens every iteration anyway
      return prngs.contains(random);
    }
    final boolean eligible;
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

  @SuppressWarnings({"InfiniteLoopStatement", "ObjectAllocationInLoop", "AwaitNotInLoop"})
  @Override
  public boolean iterate() throws InterruptedException {
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
    final Iterator<Random> iterator = prngsThisIteration.iterator();
    boolean entropyConsumed = false;
    while (iterator.hasNext()) {
      final Random random = iterator.next();
      iterator.remove();
      if (random instanceof EntropyCountingRandom
          && ((EntropyCountingRandom) random).entropyBits() > 0) {
        continue;
      } else {
        entropyConsumed = true;
      }
      try {
        if (random instanceof ByteArrayReseedableRandom && !((ByteArrayReseedableRandom) random)
            .preferSeedWithLong()) {
          final ByteArrayReseedableRandom reseedable = (ByteArrayReseedableRandom) random;
          final byte[] seedArray = seedArrays.computeIfAbsent(reseedable, random_ ->
              new byte[random_.getNewSeedLength()]);
          seedGenerator.generateSeed(seedArray);
          reseedable.setSeed(seedArray);
        } else {
          seedGenerator.generateSeed(longSeedArray);
          random.setSeed(longSeedBuffer.getLong(0));
        }
      } catch (final Throwable t) {
        LOG.error("%s", t);
        for (final StackTraceElement element : t.getStackTrace()) {
          LOG.error("  %s", element);
        }
        interrupt();
      }
    }
    if (!entropyConsumed) {
      waitForEntropyDrain.await();
    }
    return true;
  }

  /**
   * <p>isEmpty.</p>
   *
   * @return a boolean.
   */
  public boolean isEmpty() {
    synchronized (prngs) {
      return prngs.isEmpty();
    }
  }

  /**
   * Add one or more {@link java.util.Random} instances. The caller must not hold locks on any of
   * these instances that are also acquired during {@link java.util.Random#setSeed(long)} or {@link
   * ByteArrayReseedableRandom#setSeed(byte[])}, as one of those methods may be called immediately
   * and this would cause a circular deadlock.
   *
   * @param randoms a {@link java.util.Random} object.
   */
  public void add(final Random... randoms) {
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

  /**
   * <p>remove.</p>
   *
   * @param randoms a {@link java.util.Random} object.
   */
  public void remove(final Random... randoms) {
    prngs.removeAll(Arrays.asList(randoms));
  }

  /**
   * Shut down this thread if no {@link Random} instances are registered with it.
   */
  public void stopIfEmpty() {
    if (isEmpty()) {
      LOG.info("Stopping empty RandomSeederThread for %s", seedGenerator);
      interrupt();
    }
  }
}
