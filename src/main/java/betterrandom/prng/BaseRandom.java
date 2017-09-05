package betterrandom.prng;

import betterrandom.ByteArrayReseedableRandom;
import betterrandom.RepeatableRandom;
import betterrandom.seed.DefaultSeedGenerator;
import betterrandom.seed.SeedException;
import betterrandom.seed.SeedGenerator;
import betterrandom.util.BinaryUtils;
import betterrandom.util.Dumpable;
import betterrandom.util.LogPreFormatter;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

public abstract class BaseRandom extends Random implements ByteArrayReseedableRandom,
    RepeatableRandom, Dumpable {

  public abstract ToStringHelper addSubclassFields(ToStringHelper original);

  public String dump() {
    lock.lock();
    try {
      return addSubclassFields(MoreObjects.toStringHelper(this)
          .add("seed", BinaryUtils.convertBytesToHexString(seed)))
          .toString();
    } finally {
      lock.unlock();
    }
  }

  private static final LogPreFormatter LOG = new LogPreFormatter(BaseRandom.class);
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
    superConstructorFinished = true;
    if (seed == null) {
      throw new IllegalArgumentException("Seed must not be null");
    }
    initTransientFields();
    setSeedInitial(seed);
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

  @Override
  public synchronized void setSeed(@UnknownInitialization(Random.class) BaseRandom this,
      long seed) {
    ByteBuffer buffer = ByteBuffer.allocate(8);
    buffer.putLong(seed);
    byte[] array = buffer.array();
    setSeedMaybeInitial(array);
  }

  @SuppressWarnings("method.invocation.invalid")
  @EnsuresNonNull("this.seed")
  protected void setSeedMaybeInitial(@UnknownInitialization(Random.class)BaseRandom this,
      byte[] seed) {
    if (superConstructorFinished) {
      setSeed(seed);
    } else {
      setSeedInitial(seed);
    }
  }

  @EnsuresNonNull("this.seed")
  @Override
  public void setSeed(byte[] seed) {
    lock.lock();
    try {
      setSeedInitial(seed);
    } finally {
      lock.unlock();
    }
  }

  @EnsuresNonNull("this.seed")
  public void setSeedInitial(@UnknownInitialization(Random.class) BaseRandom this, byte[] seed) {
    this.seed = seed.clone();
  }

  @EnsuresNonNull("lock")
  protected void initTransientFields(@UnknownInitialization BaseRandom this) {
    if (lock == null) {
      lock = new ReentrantLock();
    }
    superConstructorFinished = true;
  }

  @EnsuresNonNull({"lock", "seed"})
  private void readObject(@UnderInitialization(BaseRandom.class)BaseRandom this,
      ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    assert seed != null : "@AssumeAssertion(nullness)";
    initTransientFields();
    setSeedInitial(seed);
  }

  @EnsuresNonNull({"lock", "seed"})
  @SuppressWarnings({"OverriddenMethodCallDuringObjectConstruction"})
  protected void readObjectNoData() throws InvalidObjectException {
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
}
