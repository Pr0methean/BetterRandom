package betterrandom.prng;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

public abstract class BaseRandom extends Random {

  private static final Logger LOG = Logger.getLogger(BaseRandom.class.getName());
  private static final long serialVersionUID = -1556392727255964947L;
  protected static final SecureRandom SECURE_RANDOM = new SecureRandom();

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
  public BaseRandom(int seedLength) {
    this(SECURE_RANDOM.generateSeed(seedLength));
  }

  @EnsuresNonNull("this.seed")
  public BaseRandom(byte[] seed) {
    if (seed == null) {
      throw new IllegalArgumentException("Seed must not be null");
    }
    this.seed = seed.clone();
    initTransientFields();
  }

  @EnsuresNonNull({"seed", "lock"})
  protected void checkedReadObject(@UnknownInitialization BaseRandom this,
      ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    assert seed != null : "@AssumeAssertion(nullness)";
    initTransientFields();
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
  @EnsuresNonNull("this.seed")
  @RequiresNonNull({"lock"})
  public void setSeed(@UnknownInitialization(BaseRandom.class)BaseRandom this, byte[] seed) {
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
  private void readObject(@UnderInitialization(BaseRandom.class)BaseRandom this,
      ObjectInputStream in) throws IOException, ClassNotFoundException {
    checkedReadObject(in);
    initTransientFields();
  }

  @EnsuresNonNull({"lock", "seed"})
  @SuppressWarnings({"OverriddenMethodCallDuringObjectConstruction"})
  private void readObjectNoData() throws InvalidObjectException {
    this.seed = SECURE_RANDOM.generateSeed(getNewSeedLength());
    initTransientFields();
  }

  protected abstract int getNewSeedLength();
}
