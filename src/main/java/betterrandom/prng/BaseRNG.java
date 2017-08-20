package betterrandom.prng;

import betterrandom.ByteArrayReseedableRandom;
import betterrandom.EntropyCountingRandom;
import betterrandom.seed.DefaultSeedGenerator;
import betterrandom.seed.SeedException;
import betterrandom.seed.SeedGenerator;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class BaseRNG extends Random implements ByteArrayReseedableRandom,
    EntropyCountingRandom {

  protected byte[] seed;
  // Lock to prevent concurrent modification of the RNG's internal state.
  protected transient Lock lock;
  protected transient boolean superConstructorFinished = false;

  /**
   * Creates a new RNG and seeds it using the default seeding strategy.
   */
  public BaseRNG(int seedLength) {
    this(DefaultSeedGenerator.getInstance().generateSeed(seedLength));
  }

  /**
   * Seed the RNG using the provided seed generation strategy.
   *
   * @param seedGenerator The seed generation strategy that will provide the seed value for this
   * RNG.
   * @throws SeedException If there is a problem generating a seed.
   */
  public BaseRNG(SeedGenerator seedGenerator, int seedLength) throws SeedException {
    this(seedGenerator.generateSeed(seedLength));
  }

  public BaseRNG(byte[] seed) {
    if (seed == null) {
      throw new IllegalArgumentException("Seed must not be null");
    }
    this.seed = seed;
    initTransientFields();
  }

  protected synchronized void initTransientFields() {
    if (lock == null) {
      lock = new ReentrantLock(); 
    }
    superConstructorFinished = true;
  }

  private void readObject(ObjectInputStream in) throws IOException,
      ClassNotFoundException {
    in.defaultReadObject();
    initTransientFields();
  }

  @Override
  public void setSeed(long seed) {
    ByteBuffer buffer = ByteBuffer.allocate(8);
    buffer.putLong(seed);
    setSeed(buffer.array());
  }

  @Override
  public void setSeed(byte[] seed) {
    lock.lock();
    try {
      this.seed = seed.clone();
    } finally {
      lock.unlock();
    }
  }
}
