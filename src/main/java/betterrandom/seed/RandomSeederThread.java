package betterrandom.seed;

import betterrandom.ByteArrayReseedableRandom;
import betterrandom.EntropyCountingRandom;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class RandomSeederThread extends Thread implements Serializable {

  private static final Logger LOG = Logger.getLogger(RandomSeederThread.class.getName());
  private static final Map<SeedGenerator, RandomSeederThread> INSTANCES =
      new ConcurrentHashMap<>();
  /**
   * Used to avoid full spin-locking when every {@link Random} to be reseeded is an {@link
   * EntropyCountingRandom} and none has spent its entropy.
   */
  private static final long ENTROPY_POLL_INTERVAL_MS = 10;
  private static final long serialVersionUID = -2858126391794302039L;
  private final SeedGenerator seedGenerator;
  @SuppressWarnings("NonSerializableFieldInSerializableClass")
  private transient Set<Random> prngs;
  private final byte[] seedArray = new byte[8];
  private transient ByteBuffer seedBuffer;

  /**
   * Private constructor because only one instance per seed source.
   */
  private RandomSeederThread(SeedGenerator seedGenerator) {
    this.seedGenerator = seedGenerator;
    initTransientState();
  }

  /**
   * Obtain the instance for the given {@link SeedGenerator}, creating it if it doesn't exist.
   */
  public static RandomSeederThread getInstance(SeedGenerator seedGenerator) {
    return INSTANCES.computeIfAbsent(seedGenerator, RandomSeederThread::new);
  }

  private void initTransientState() {
    prngs = Collections.newSetFromMap(
      Collections.synchronizedMap(new WeakHashMap<>()));
    seedBuffer = ByteBuffer.wrap(seedArray);
    setDaemon(true);
    start();
  }

  private void writeObject(ObjectOutputStream oos) throws IOException {
    oos.defaultWriteObject();
    oos.writeObject(new HashMap<>(
        prngs.stream().collect(Collectors.toMap(Function.identity(), prng -> true))));
  }

  @SuppressWarnings("unchecked")
  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    ois.defaultReadObject();
    initTransientState();
    prngs.addAll(((Map<Random,?>) ois.readObject()).keySet());
  }

  @SuppressWarnings("InfiniteLoopStatement")
  @Override
  public void run() {
    try {
      while (true) {
        while (isEmpty()) {
          wait();
        }
        boolean entropyConsumed = false;
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
        if (!entropyConsumed) {
          Thread.sleep(ENTROPY_POLL_INTERVAL_MS);
        }
      }
    } catch (InterruptedException e) {
      INSTANCES.remove(seedGenerator);
    }
  }

  public synchronized boolean isEmpty() {
    return prngs.isEmpty();
  }

  /**
   * Add one or more {@link Random} instances.
   */
  public synchronized void add(Random... randoms) {
    if (!isAlive()) {
      throw new IllegalStateException("Already shut down");
    }
    for (Random random : randoms) {
      prngs.add(random);
    }
    notifyAll();
  }

  public void stopIfEmpty() {
    if (isEmpty()) {
      interrupt();
    }
  }
}
