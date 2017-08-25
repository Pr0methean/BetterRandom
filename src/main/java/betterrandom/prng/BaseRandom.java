package betterrandom.prng;

import betterrandom.ByteArrayReseedableRandom;
import betterrandom.RepeatableRandom;
import betterrandom.seed.DefaultSeedGenerator;
import betterrandom.seed.SeedException;
import betterrandom.seed.SeedGenerator;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

public abstract class BaseRandom extends Random implements ByteArrayReseedableRandom,
    RepeatableRandom {

  private static final Logger LOG = Logger.getLogger(BaseRandom.class.getName());
  private static final long serialVersionUID = -1556392727255964947L;

  protected byte[] seed;

  // Lock to prevent concurrent modification of the RNG's internal state.
  @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
  protected transient Lock lock;
  /**
   * Use this to ignore setSeed(long) calls from super constructor
   */
  @SuppressWarnings({"InstanceVariableMayNotBeInitializedByReadObject",
      "FieldAccessedSynchronizedAndUnsynchronized"})
  protected transient boolean superConstructorFinished = false;

  /**
   * Creates a new RNG and seeds it using the default seeding strategy.
   */
  public BaseRandom(int seedLength) throws SeedException {
    this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(seedLength));
  }

  /**
   * Seed the RNG using the provided seed generation strategy.
   *
   * @param seedGenerator The seed generation strategy that will provide the seed value for this
   * RNG.
   * @throws SeedException If there is a problem generating a seed.
   */
  public BaseRandom(SeedGenerator seedGenerator, int seedLength) throws SeedException {
    this(seedGenerator.generateSeed(seedLength));
  }

  @EnsuresNonNull("this.seed")
  public BaseRandom(byte[] seed) {
    if (seed == null) {
      throw new IllegalArgumentException("Seed must not be null");
    }
    this.seed = seed.clone();
    initTransientFields();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] getSeed() {
    lock.lock();
    try {
      return seed.clone();
    } finally {
      lock.unlock();
    }
  }

  @SuppressWarnings("contracts.postcondition.not.satisfied")
  @EnsuresNonNull("this.seed")
  @Override
  public synchronized void setSeed(@UnknownInitialization BaseRandom this, long seed) {
    if (superConstructorFinished) {
      assert lock != null : "@AssumeAssertion(nullness)";
      // setSeed can't work until seed array allocated
      ByteBuffer buffer = ByteBuffer.allocate(8);
      buffer.putLong(seed);
      setSeed(buffer.array());
    }
  }

  // Checker Framework doesn't recognize that the @UnknownInitialization weakens the precondition
  // even with the @RequiresNonNull
  @SuppressWarnings("contracts.precondition.override.invalid")
  @EnsuresNonNull("this.seed")
  @RequiresNonNull({"lock"})
  @Override
  public void setSeed(@UnknownInitialization BaseRandom this, byte[] seed) {
    lock.lock();
    try {
      this.seed = seed.clone();
    } finally {
      lock.unlock();
    }
    assert this.seed != null : "@AssumeAssertion(nullness)";
  }

  @EnsuresNonNull("lock")
  private void initTransientFields(@UnknownInitialization BaseRandom this) {
    if (lock == null) {
      lock = new ReentrantLock();
    }
    superConstructorFinished = true;
  }

  @EnsuresNonNull({"lock", "seed"})
  private void readObject(ObjectInputStream in) throws IOException,
      ClassNotFoundException {
    in.defaultReadObject();
    initTransientFields();
  }

  @EnsuresNonNull({"lock", "seed"})
  @SuppressWarnings({"unused", "OverriddenMethodCallDuringObjectConstruction"})
  private void readObjectNoData() throws InvalidObjectException {
    LOG.warning("BaseRandom.readObjectNoData() invoked; using DefaultSeedGenerator");
    try {
      setSeed(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(getNewSeedLength()));
    } catch (SeedException e) {
      throw (InvalidObjectException)
          (new InvalidObjectException("Unable to deserialize or generate a seed this RNG")
              .initCause(e));
    }
    initTransientFields();
  }
}
