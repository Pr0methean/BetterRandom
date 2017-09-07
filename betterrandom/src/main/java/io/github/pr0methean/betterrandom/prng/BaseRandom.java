package io.github.pr0methean.betterrandom.prng;

import static org.checkerframework.checker.nullness.NullnessUtil.castNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import io.github.pr0methean.betterrandom.RepeatableRandom;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.Dumpable;
import io.github.pr0methean.betterrandom.util.LogPreFormatter;
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
  protected transient byte[] longSeedArray = new byte[8];
  protected transient ByteBuffer longSeedBuffer = ByteBuffer.wrap(longSeedArray);

  /**
   * The purpose of this method is so that entropy-counting subclasses can detect that no more than
   * 1 bit of entropy is actually being consumed, even though this method uses {@link #nextDouble()}
   * which normally consumes 53 bits. Tracking of fractional bits of entropy is currently not
   * implemented in {@link BaseEntropyCountingRandom}.
   *
   * @param probability The probability of returning true.
   * @return True with probability {@code probability}; false otherwise. If {@code probability < 0},
   *     always returns false. If {@code probability >= 1}, always returns true.
   */
  public final boolean withProbability(double probability) {
    if (probability <= 0) {
      return false;
    } else if (probability >= 1) {
      return true;
    } else {
      return withProbabilityInternal(probability);
    }
  }

  /**
   * Called by {@link #withProbability(double)} to generate a boolean with a specified probability
   * of returning true, after special cases (probability 0 or less, or 1 or more) have been
   * eliminated.
   *
   * @param probability The probability of returning true; always strictly between 0 and 1.
   * @return True with probability {@code probability}; false otherwise.
   */
  protected boolean withProbabilityInternal(double probability) {
    return nextDouble() <= probability;
  }

  /**
   * Creates a new RNG and seeds it using the default seeding strategy.
   */
  public BaseRandom(final int seedLength) throws SeedException {
    this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(seedLength));
  }

  /**
   * Seed the RNG using the provided seed generation strategy.
   *
   * @param seedGenerator The seed generation strategy that will provide the seed value for this
   *     RNG.
   * @throws SeedException If there is a problem generating a seed.
   */
  public BaseRandom(final SeedGenerator seedGenerator, final int seedLength) throws SeedException {
    this(seedGenerator.generateSeed(seedLength));
  }

  @EnsuresNonNull("this.seed")
  public BaseRandom(final byte[] seed) {
    superConstructorFinished = true;
    if (seed == null) {
      throw new IllegalArgumentException("Seed must not be null");
    }
    initTransientFields();
    setSeedInternal(seed);
  }

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
  public void setSeed(final byte[] seed) {
    lock.lock();
    try {
      setSeedInternal(seed);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public synchronized void setSeed(@UnknownInitialization(Random.class)BaseRandom this,
      final long seed) {
    longSeedBuffer.putLong(seed);
    setSeedMaybeInitial(longSeedArray);
  }

  @SuppressWarnings("method.invocation.invalid")
  @EnsuresNonNull("this.seed")
  protected void setSeedMaybeInitial(@UnknownInitialization(Random.class)BaseRandom this,
      final byte[] seed) {
    if (superConstructorFinished) {
      setSeed(seed);
    } else {
      setSeedInternal(seed);
    }
  }

  /**
   * Sets the seed, and should be overridden to set other state that derives from the seed. Called
   * by {@link #setSeed(byte[])}, whose default implementation ensures that the lock is held while
   * doing so. Also called by constructors, {@link #readObject(ObjectInputStream)} and {@link
   * #readObjectNoData()}.
   *
   * @param seed The new seed.
   */
  @EnsuresNonNull("this.seed")
  protected void setSeedInternal(@UnknownInitialization(Random.class)BaseRandom this,
      final byte[] seed) {
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
      final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    initTransientFields();
    setSeedInternal(castNonNull(seed));
  }

  @EnsuresNonNull({"lock", "seed"})
  @SuppressWarnings("OverriddenMethodCallDuringObjectConstruction")
  protected void readObjectNoData() throws InvalidObjectException {
    LOG.warn("BaseRandom.readObjectNoData() invoked; using DefaultSeedGenerator");
    try {
      setSeed(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(getNewSeedLength()));
    } catch (final SeedException e) {
      throw (InvalidObjectException)
          (new InvalidObjectException("Unable to deserialize or generate a seed this RNG")
              .initCause(e));
    }
    initTransientFields();
  }
}
