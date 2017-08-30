package betterrandom.prng;

import betterrandom.ByteArrayReseedableRandom;
import betterrandom.RepeatableRandom;
import betterrandom.seed.DefaultSeedGenerator;
import betterrandom.seed.SeedException;
import betterrandom.seed.SeedGenerator;
import betterrandom.util.LogPreFormatter;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

public abstract class BaseRandom extends Random implements ByteArrayReseedableRandom,
    RepeatableRandom {

  private static final LogPreFormatter LOG = new LogPreFormatter(BaseRandom.class);
  private static final long serialVersionUID = -1556392727255964947L;

  protected byte[] seed;
  protected Integer hashCode;

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
  @SuppressWarnings("method.invocation.invalid")
  public BaseRandom(byte[] seed) {
    superConstructorFinished = true;
    if (seed == null) {
      throw new IllegalArgumentException("Seed must not be null");
    }
    initTransientFields();
    setSeed(seed);
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
  public synchronized void setSeed(@UnknownInitialization(BaseRandom.class)BaseRandom this,
      long seed) {
    if (superConstructorFinished) {
      assert lock != null : "@AssumeAssertion(nullness)";
      ByteBuffer buffer = ByteBuffer.allocate(8);
      buffer.putLong(seed);
      setSeed(buffer.array());
    }
  }

  // Checker Framework doesn't recognize that the @UnknownInitialization weakens the precondition
  // even with the @RequiresNonNull
  @SuppressWarnings("contracts.precondition.override.invalid")
  @EnsuresNonNull({"this.seed", "hashCode"})
  @RequiresNonNull({"lock"})
  @Override
  public void setSeed(@UnknownInitialization(BaseRandom.class)BaseRandom this, byte[] seed) {
    lock.lock();
    try {
      this.seed = seed.clone();
      if (hashCode == null) {
        hashCode = Arrays.hashCode(seed);
      }
    } finally {
      lock.unlock();
    }
    assert this.seed != null : "@AssumeAssertion(nullness)";
    assert hashCode != null : "@AssumeAssertion(nullness)";
  }

  @EnsuresNonNull("lock")
  protected void initTransientFields(@UnknownInitialization BaseRandom this) {
    if (lock == null) {
      lock = new ReentrantLock();
    }
  }

  @EnsuresNonNull({"lock", "seed"})
  private void readObject(@UnderInitialization(BaseRandom.class)BaseRandom this,
      ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    assert seed != null : "@AssumeAssertion(nullness)";
    initTransientFields();
  }

  @EnsuresNonNull({"lock", "seed"})
  @SuppressWarnings({"OverriddenMethodCallDuringObjectConstruction"})
  private void readObjectNoData() throws InvalidObjectException {
    LOG.warn("BaseRandom.readObjectNoData() invoked; using DefaultSeedGenerator");
    try {
      setSeed(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(getNewSeedLength()));
    } catch (SeedException e) {
      throw (InvalidObjectException)
          (new InvalidObjectException("Unable to deserialize or generate a seed this RNG")
              .initCause(e));
    }
    initTransientFields();
  }

  @Override
  public boolean equals(@Nullable Object o) {
    lock.lock();
    try {
      return o != null
          && getClass().equals(o.getClass())
          && Arrays.equals(seed, ((BaseRandom) o).seed);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public int hashCode() {
    if (hashCode == null) {
      LOG.warn("hashCode() called prematurely on %s", this);
      return 0;
    } else {
      return hashCode;
    }
  }
}
