package betterrandom.seed;

import betterrandom.ByteArrayReseedableRandom;
import betterrandom.EntropyCountingRandom;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

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
  private final byte[] seedArray = new byte[8];
  @SuppressWarnings({"NonSerializableFieldInSerializableClass",
      "InstanceVariableMayNotBeInitializedByReadObject"})
  private transient Set<Random> prngs;
  @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
  private transient ByteBuffer seedBuffer;

  /**
   * Private constructor because only one instance per seed source.
   */
  private RandomSeederThread(SeedGenerator seedGenerator) {
    this.seedGenerator = seedGenerator;
    initTransientState();
  }

  /**
   * Obtain the instance for the given {@link SeedGenerator}, creating and starting it if it doesn't
   * exist.
   */
  public static RandomSeederThread getInstance(SeedGenerator seedGenerator) {
    RandomSeederThread thread = INSTANCES.computeIfAbsent(
        Objects.requireNonNull(seedGenerator, "seedGenerator must not be null"),
        RandomSeederThread::new);
    thread.setDaemon(true);
    thread.start();
    return thread;
  }

  /**
   * Ensure one instance per SeedGenerator even after deserialization.
   */
  @SuppressWarnings("unused")
  private RandomSeederThread readResolve() {
    return getInstance(seedGenerator);
  }

  @EnsuresNonNull({"seedBuffer", "prngs"})
  private void initTransientState(@UnderInitialization RandomSeederThread this) {
    prngs = Collections.newSetFromMap(
        Collections.synchronizedMap(new WeakHashMap<>()));
    seedBuffer = ByteBuffer.wrap(seedArray);
  }

  private void writeObject(ObjectOutputStream oos) throws IOException {
    oos.defaultWriteObject();
    oos.writeObject(new ArrayList<>(prngs));
  }

  @SuppressWarnings("unchecked")
  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    ois.defaultReadObject();
    getInstance(seedGenerator).prngs.addAll((ArrayList<Random>) (ois.readObject()));
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
      interrupt();
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

  @Override
  public String toString() {
    return "RandomSeederThread for " + seedGenerator;
  }
}
