package betterrandom.prng;

import betterrandom.ByteArrayReseedableRandom;
import betterrandom.EntropyCountingRandom;
import betterrandom.seed.DefaultSeedGenerator;
import betterrandom.seed.SeedException;
import betterrandom.seed.SeedGenerator;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

public abstract class BaseRNG extends Random implements ByteArrayReseedableRandom,
    EntropyCountingRandom {

  protected byte[] seed;
  // Lock to prevent concurrent modification of the RNG's internal state.
  protected transient Lock lock;
  protected transient boolean superConstructorFinished = false;

  private static final HashMap<Class<? extends BaseRNG>, Integer> seedLengthsForClass = new HashMap<>();
  private static final Objenesis objenesis = new ObjenesisStd();

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
    this.seed = seed.clone();
  }

  protected void initTransientFields() {
    lock = new ReentrantLock();
    superConstructorFinished = true;
  }

  protected void readObject(ObjectInputStream in) throws IOException,
      ClassNotFoundException {
    in.defaultReadObject();
    initTransientFields();
  }

  @Override
  public void setSeed(byte[] seed) {
    lock.lock();
    this.seed = seed;
    lock.unlock();
  }
}
