package betterrandom.seed;

import betterrandom.ByteArrayReseedableRandom;
import betterrandom.EntropyCountingRandom;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class RandomSeederThread extends Thread {

  private static final Logger LOG = Logger.getLogger(RandomSeederThread.class.getName());
  private static final Map<SeedGenerator, RandomSeederThread> INSTANCES =
      new ConcurrentHashMap<>();
  /**
   * Used to avoid full spin-locking when every {@link Random} to be reseeded is an {@link
   * EntropyCountingRandom} and none has spent its entropy.
   */
  private static final long ENTROPY_POLL_INTERVAL_MS = 10;
  private final SeedGenerator seedGenerator;
  private final byte[] seedArray = new byte[8];
  private final Set<Random> prngs = Collections.synchronizedSet(
      Collections.newSetFromMap(new WeakHashMap<>()));
  private ByteBuffer seedBuffer = ByteBuffer.wrap(seedArray);

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
    RandomSeederThread thread = INSTANCES.computeIfAbsent(seedGenerator,
        RandomSeederThread::new);
    thread.setName("RandomSeederThread for " + seedGenerator);
    thread.setDaemon(true);
    thread.start();
    return thread;
  }

  @SuppressWarnings("InfiniteLoopStatement")
  @Override
  public void run() {
    try {
      while (true) {
        while (isEmpty()) {
          synchronized (this) {
            wait();
          }
        }
        boolean entropyConsumed = false;
        synchronized (prngs) {
          for (Random random : prngs) {
            if (random instanceof EntropyCountingRandom
                && ((EntropyCountingRandom) random).entropyOctets() > 0) {
              continue;
            }
            try {
              if (!(random instanceof EntropyCountingRandom)
                  || ((EntropyCountingRandom) random).entropyOctets() > 0) {
                entropyConsumed = true;
                if (random instanceof ByteArrayReseedableRandom) {
                  ByteArrayReseedableRandom reseedable = (ByteArrayReseedableRandom) random;
                  reseedable.setSeed(seedGenerator.generateSeed(reseedable.getNewSeedLength()));
                } else {
                  synchronized (this) {
                    System.arraycopy(seedGenerator.generateSeed(8), 0, seedArray, 0, 8);
                    random.setSeed(seedBuffer.getLong(0));
                  }
                }
              }
            } catch (SeedException e) {
              LOG.severe(String.format("%s gave SeedException %s", seedGenerator, e));
              interrupt();
            }
          }
        }
        if (!entropyConsumed) {
          Thread.sleep(ENTROPY_POLL_INTERVAL_MS);
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
  public synchronized void add(Random... randoms) {
    if (isInterrupted()) {
      throw new IllegalStateException("Already shut down");
    }
    Collections.addAll(prngs, randoms);
    notifyAll();
  }

  public void stopIfEmpty() {
    if (isEmpty()) {
      interrupt();
    }
  }
}
