package betterrandom.seed;

import betterrandom.ByteArrayReseedableRandom;
import betterrandom.EntropyCountingRandom;
import betterrandom.util.LogPreFormatter;
import betterrandom.util.LooperThread;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

@SuppressWarnings("ClassExplicitlyExtendsThread")
public final class RandomSeederThread extends LooperThread {

  private static final LogPreFormatter LOG
      = new LogPreFormatter(Logger.getLogger(RandomSeederThread.class.getName()));
  private static final Map<SeedGenerator, RandomSeederThread> INSTANCES =
      Collections.synchronizedMap(new WeakHashMap<>());
  /**
   * Used to avoid full spin-locking when every {@link Random} to be reseeded is an {@link
   * EntropyCountingRandom} and none has spent its entropy.
   */
  private static final long POLL_INTERVAL_MS = 1000;
  private final SeedGenerator seedGenerator;
  private final byte[] seedArray = new byte[8];
  private transient Set<Random> prngs;
  private @Nullable Set<Random> prngsSerial;
  private transient ByteBuffer seedBuffer;
  private transient Condition waitWhileEmpty;
  private transient Condition waitForEntropyDrain;

  /**
   * Private constructor because only one instance per seed source.
   */
  private RandomSeederThread(SeedGenerator seedGenerator) {
    this.seedGenerator = seedGenerator;
    initTransientFields();
  }

  @EnsuresNonNull({"prngs", "seedBuffer", "waitWhileEmpty", "waitForEntropyDrain"})
  @RequiresNonNull("lock")
  private void initTransientFields(@UnderInitialization RandomSeederThread this) {
    prngs = Collections.synchronizedSet(
        Collections.newSetFromMap(new WeakHashMap<>()));
    seedBuffer = ByteBuffer.wrap(seedArray);
    waitWhileEmpty = lock.newCondition();
    waitForEntropyDrain = lock.newCondition();
  }

  private void readObject(@UnderInitialization RandomSeederThread this,
      ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    assert lock != null : "@AssumeAssertion(nullness)";
    initTransientFields();
    if (prngsSerial != null) {
      prngs.addAll(prngsSerial);
    }
    prngsSerial = null;
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    prngsSerial = new HashSet<>();
    prngsSerial.addAll(prngs);
    out.defaultWriteObject();
    prngsSerial = null;
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

  /**
   * Asynchronously triggers reseeding of the given {@link EntropyCountingRandom} if it is
   * associated with a live RandomSeederThread.
   *
   * @return Whether or not the reseed was successfully scheduled.
   */
  public boolean asyncReseed(EntropyCountingRandom random) {
    boolean eligible;
    synchronized (prngs) {
      eligible = prngs.contains(random);
    }
    if (eligible) {
      lock.lock();
      try {
        waitForEntropyDrain.signalAll();
      } finally {
        lock.unlock();
      }
      return true;
    } else {
      return false;
    }
  }

  @SuppressWarnings("InfiniteLoopStatement")
  @Override
  public void iterate() throws InterruptedException {
    while (isEmpty()) {
      waitWhileEmpty.await();
    }
    boolean entropyConsumed = false;
    synchronized (prngs) {
      for (Random random : prngs) {
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
            //noinspection CallToNativeMethodWhileLocked
            System.arraycopy(seedGenerator.generateSeed(8), 0, seedArray, 0, 8);
            random.setSeed(seedBuffer.getLong(0));
          }
        } catch (SeedException e) {
          //noinspection AccessToStaticFieldLockedOnInstance
          LOG.error("%s gave SeedException %s", seedGenerator, e);
          interrupt();
        }
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
   * Add one or more {@link Random} instances.
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
