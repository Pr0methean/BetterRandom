package betterrandom.seed;

import betterrandom.ByteArrayReseedableRandom;
import betterrandom.EntropyCountingRandom;
import java.nio.ByteBuffer;
import java.util.Collections;
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

@SuppressWarnings("ClassExplicitlyExtendsThread")
public final class RandomSeederThread extends Thread {

  private static final Logger LOG = Logger.getLogger(RandomSeederThread.class.getName());
  private static final Map<SeedGenerator, RandomSeederThread> INSTANCES =
      Collections.synchronizedMap(new WeakHashMap<>());
  /**
   * Used to avoid full spin-locking when every {@link Random} to be reseeded is an {@link
   * EntropyCountingRandom} and none has spent its entropy.
   */
  private static final long ENTROPY_POLL_INTERVAL_MS = 1000;
  private final SeedGenerator seedGenerator;
  private final byte[] seedArray = new byte[8];
  private final Set<Random> prngs = Collections.synchronizedSet(
      Collections.newSetFromMap(new WeakHashMap<>()));
  private final ByteBuffer seedBuffer = ByteBuffer.wrap(seedArray);
  private final Lock lock = new ReentrantLock();
  private final Condition waitWhileEmpty = lock.newCondition();
  private final Condition waitForEntropyDrain = lock.newCondition();

  /**
   * Private constructor because only one instance per seed source.
   */
  private RandomSeederThread(SeedGenerator seedGenerator) {
    this.seedGenerator = seedGenerator;
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
  public void run() {
    try {
      while (true) {
        while (isEmpty()) {
          lock.lock();
          try {
            waitWhileEmpty.await();
          } finally {
            lock.unlock();
          }
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
              LOG.severe(String.format("%s gave SeedException %s", seedGenerator, e));
              interrupt();
            }
          }
        }
        if (!entropyConsumed) {
          lock.lock();
          try {
            waitForEntropyDrain.await(ENTROPY_POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
          } finally {
            lock.unlock();
          }
        }
      }
    } catch (InterruptedException e) {
      interrupt();
      INSTANCES.remove(seedGenerator);
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

  public void stopIfEmpty() {
    if (isEmpty()) {
      interrupt();
    }
  }
}
