package betterrandom.seed;

import betterrandom.ByteArrayReseedableRandom;
import betterrandom.EntropyCountingRandom;
import betterrandom.util.WeakReferenceWithEquals;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class RandomSeederThread extends Thread {

  private static final Logger LOG = Logger.getLogger(RandomSeederThread.class.getName());
  private static final Map<SeedGenerator, RandomSeederThread> INSTANCES =
      new ConcurrentHashMap<>();
  private final SeedGenerator seedGenerator;
  private final Set<WeakReferenceWithEquals<Random>> prngs = Collections.newSetFromMap(
      new ConcurrentHashMap<WeakReferenceWithEquals<Random>, Boolean>());
  private final ByteBuffer seedBuffer = ByteBuffer.allocate(8);
  private final byte[] seedArray = seedBuffer.array();

  /**
   * Private constructor because only one instance per seed source.
   */
  private RandomSeederThread(SeedGenerator seedGenerator) {
    setDaemon(true);
    this.seedGenerator = seedGenerator;
    start();
  }

  /**
   * Obtain the instance for the given {@link SeedGenerator}, creating it if it doesn't exist.
   */
  public static RandomSeederThread getInstance(SeedGenerator seedGenerator) {
    RandomSeederThread thread = INSTANCES.get(seedGenerator);
    if (thread == null) {
      thread = new RandomSeederThread(seedGenerator);
      INSTANCES.put(seedGenerator, thread);
    }
    return thread;
  }

  @SuppressWarnings("InfiniteLoopStatement")
  @Override
  public void run() {
    try {
      while (true) {
        while (prngs.isEmpty()) {
          wait();
        }
        for (WeakReference<Random> randomRef : prngs) {
          Random random = randomRef.get();
          if (random == null) {
            // Don't keep iterating over a cleared reference
            prngs.remove(randomRef);
          } else {
            if (random instanceof EntropyCountingRandom
                && ((EntropyCountingRandom) random).entropyOctets() > 0) {
              continue;
            }
            try {
              if (random instanceof ByteArrayReseedableRandom) {
                ByteArrayReseedableRandom reseedable = (ByteArrayReseedableRandom) random;
                reseedable.setSeed(seedGenerator.generateSeed(reseedable.getNewSeedLength()));
              } else {
                synchronized (this) {
                  System.arraycopy(seedGenerator.generateSeed(8), 0, seedArray, 0, 8);
                  random.setSeed(seedBuffer.getLong(0));
                }
              }
            } catch (SeedException e) {
              LOG.severe(String.format("%s gave SeedException %s", seedGenerator, e));
              interrupt();
            }
          }
        }
      }
    } catch (InterruptedException e) {
      INSTANCES.remove(seedGenerator);
    }
  }

  /**
   * Add one or more {@link Random} instances.
   */
  public void add(Random... randoms) {
    for (Random random : randoms) {
      prngs.add(new WeakReferenceWithEquals<>(random));
    }
    notifyAll();
  }
}
