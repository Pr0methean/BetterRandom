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
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

@SuppressWarnings("OverriddenMethodCallDuringObjectConstruction")
public abstract class BaseRandom extends Random implements ByteArrayReseedableRandom,
    RepeatableRandom {

  private static final Logger LOG = Logger.getLogger(BaseRandom.class.getName());
  private static final long serialVersionUID = -1556392727255964947L;

  protected byte[] seed;

  /**
   * Override this if initTransientFields has been overridden to depend on subclasses' serial
   * fields. If doing so, you must call initTransientFields from the subclass constructor.
   */
  protected boolean letSubclassInitTransientFields(@UnderInitialization BaseRandom this) {
    return false;
  }
  
  @EnsuresNonNullIf(expression = "this.lock", result = false)
  private boolean maybeInitTransientFields() {
    boolean shouldInit = !letSubclassInitTransientFields();
    if (shouldInit) {
      initTransientFields();
    }
    return shouldInit;
  }  

  // Lock to prevent concurrent modification of the RNG's internal state.
  @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
  protected transient Lock lock;
  /**
   * Use this if necessary to ignore setSeed(long) calls from super constructor
   */
  @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
  protected transient boolean superConstructorFinished = false;

  /**
   * Creates a new RNG and seeds it using the default seeding strategy.
   */
  public BaseRandom(int seedLength) throws SeedException {
    this(DefaultSeedGenerator.getInstance().generateSeed(seedLength));
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
    maybeInitTransientFields();
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

  @EnsuresNonNull("this.seed")
  @Override
  public void setSeed(@UnknownInitialization BaseRandom this, long seed) {
    ByteBuffer buffer = ByteBuffer.allocate(8);
    buffer.putLong(seed);
    setSeed(buffer.array());
  }

  @EnsuresNonNull("this.seed")
  @Override
  public void setSeed(@UnknownInitialization BaseRandom this, byte[] seed) {
    lock.lock();
    try {
      this.seed = seed.clone();
    } finally {
      lock.unlock();
    }
  }

  @EnsuresNonNull("lock")
  protected void initTransientFields(@UnknownInitialization BaseRandom this) {
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

  @EnsuresNonNull({"lock", "seed"})
  @SuppressWarnings({"unused", "OverriddenMethodCallDuringObjectConstruction"})
  private void readObjectNoData() throws InvalidObjectException {
    LOG.warning("BaseRandom.readObjectNoData() invoked; using DefaultSeedGenerator");
    try {
      setSeed(DefaultSeedGenerator.getInstance().generateSeed(getNewSeedLength()));
    } catch (SeedException e) {
      throw (InvalidObjectException)
          (new InvalidObjectException("Unable to deserialize or generate a seed this RNG")
              .initCause(e));
    }
    initTransientFields();
  }
}
