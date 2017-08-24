package betterrandom.seed;

import betterrandom.ByteArrayReseedableRandom;
import betterrandom.EntropyCountingRandom;
import betterrandom.util.WeakReferenceWithEquals;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

@SuppressWarnings("ClassExplicitlyExtendsThread")
public final class RandomSeederThread extends Thread {

  private static final Logger LOG = Logger.getLogger(RandomSeederThread.class.getName());
  private static final Map<SeedGenerator, RandomSeederThread> INSTANCES =
      Collections.synchronizedMap(new WeakHashMap<>());
  private static final Map<EntropyCountingRandom, WeakReference<RandomSeederThread>>
      REVERSE_LOOKUP_TABLE = Collections.synchronizedMap(new WeakHashMap<>());
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
  private final Condition waitWhileEmpty = new ReentrantLock().newCondition();
  private final Condition waitForEntropyDrain = new ReentrantLock().newCondition();

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

  @SuppressWarnings("InfiniteLoopStatement")
  @Override
  public void run() {
    try {
      while (true) {
        while (isEmpty()) {
          waitWhileEmpty.await();
        }
        boolean entropyConsumed = false;
        synchronized (prngs) {
          for (Random random : prngs) {
            if (random instanceof EntropyCountingRandom
                && ((EntropyCountingRandom) random).entropyOctets() > 0) {
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
          waitForEntropyDrain.await(ENTROPY_POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
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
    for (Random random : randoms) {
      if (random instanceof EntropyCountingRandom) {
        REVERSE_LOOKUP_TABLE.put((EntropyCountingRandom) random, new WeakReference<>(this));
      }
    }
    waitForEntropyDrain.signalAll();
    waitWhileEmpty.signalAll();
  }

  /**
   * Asynchronously triggers reseeding of the given {@link EntropyCountingRandom} if it is
   * associated with a live RandomSeederThread.
   *
   * @return Whether or not the reseed was successfully scheduled.
   */
  @SuppressWarnings("SynchronizationOnStaticField")
  public static boolean asyncReseed(EntropyCountingRandom random) {
    synchronized (REVERSE_LOOKUP_TABLE) {
      RandomSeederThread thread = REVERSE_LOOKUP_TABLE.get(random).get();
      if (thread == null || thread.isInterrupted()) {
        REVERSE_LOOKUP_TABLE.remove(random);
        return false;
      } else {
        thread.waitForEntropyDrain.signalAll();
        return true;
      }
    }
  }

  public void stopIfEmpty() {
    if (isEmpty()) {
      interrupt();
    }
  }
}
