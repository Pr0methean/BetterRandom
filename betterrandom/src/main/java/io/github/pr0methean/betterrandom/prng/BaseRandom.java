package io.github.pr0methean.betterrandom.prng;

import static org.checkerframework.checker.nullness.NullnessUtil.castNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import io.github.pr0methean.betterrandom.EntropyCountingRandom;
import io.github.pr0methean.betterrandom.RepeatableRandom;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * <p>Abstract BaseRandom class.</p>
 *
 * @author ubuntu
 * @version $Id: $Id
 */
public abstract class BaseRandom extends Random implements ByteArrayReseedableRandom,
    RepeatableRandom, Dumpable, EntropyCountingRandom {

  public static final int ENTROPY_OF_DOUBLE = 53;
  protected static final long ENTROPY_OF_FLOAT = 24;
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
  protected transient byte[] longSeedArray;
  protected transient ByteBuffer longSeedBuffer;
  protected AtomicLong entropyBits;
  protected @Nullable RandomSeederThread seederThread;

  /**
   * Creates a new RNG and seeds it using the default seeding strategy.
   *
   * @param seedLength a int.
   * @throws io.github.pr0methean.betterrandom.seed.SeedException if any.
   */
  public BaseRandom(final int seedLength) throws SeedException {
    this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(seedLength));
    entropyBits = new AtomicLong(0);
  }

  /**
   * Seed the RNG using the provided seed generation strategy.
   *
   * @param seedGenerator The seed generation strategy that will provide the seed value for this
   *     RNG.
   * @param seedLength a int.
   * @throws io.github.pr0methean.betterrandom.seed.SeedException If there is a problem
   *     generating a seed.
   */
  public BaseRandom(final SeedGenerator seedGenerator, final int seedLength) throws SeedException {
    this(seedGenerator.generateSeed(seedLength));
    entropyBits = new AtomicLong(0);
  }

  /**
   * <p>Constructor for BaseRandom.</p>
   *
   * @param seed an array of byte.
   */
  @EnsuresNonNull("this.seed")
  public BaseRandom(final byte[] seed) {
    superConstructorFinished = true;
    if (seed == null) {
      throw new IllegalArgumentException("Seed must not be null");
    }
    initTransientFields();
    setSeedInternal(seed);
    entropyBits = new AtomicLong(0);
  }

  /**
   * <p>entropyOfInt.</p>
   *
   * @param origin a int.
   * @param bound a int.
   * @return a int.
   */
  protected static int entropyOfInt(final int origin, final int bound) {
    return 32 - Integer.numberOfLeadingZeros(bound - origin - 1);
  }

  /**
   * <p>entropyOfLong.</p>
   *
   * @param origin a long.
   * @param bound a long.
   * @return a int.
   */
  protected static int entropyOfLong(final long origin, final long bound) {
    return 64 - Long.numberOfLeadingZeros(bound - origin - 1);
  }

  /**
   * The purpose of this method is so that entropy-counting subclasses can detect that no more than
   * 1 bit of entropy is actually being consumed, even though this method uses {@link #nextDouble()}
   * which normally consumes 53 bits. Tracking of fractional bits of entropy is currently not
   * implemented in {@link io.github.pr0methean.betterrandom.prng.BaseRandom}.
   *
   * @param probability The probability of returning true.
   * @return True with probability {@code probability}; false otherwise. If {@code probability < 0},
   *     always returns false. If {@code probability >= 1}, always returns true.
   */
  public final boolean withProbability(final double probability) {
    return probability >= 1 || (probability > 0 && withProbabilityInternal(probability));
  }

  /**
   * Called by {@link #withProbability(double)} to generate a boolean with a specified probability
   * of returning true, after special cases (probability 0 or less, or 1 or more) have been
   * eliminated.
   *
   * @param probability The probability of returning true; always strictly between 0 and 1.
   * @return True with probability {@code probability}; false otherwise.
   */
  protected boolean withProbabilityInternal(final double probability) {
    final boolean result = nextDouble() <= probability;
    // Random.nextDouble() uses 53 bits, but we're only outputting 1, so credit the rest back
    // TODO: Maybe track fractional bits of entropy in a fixed-point form?
    recordEntropySpent(-52);
    return result;
  }

  public final ToStringHelper addSubclassFields(final ToStringHelper original) {
    return addSubSubclassFields(original
        .add("entropyBits", entropyBits.get())
        .add("seederThread", seederThread));
  }

  /**
   * <p>dump.</p>
   *
   * @return a {@link java.lang.String} object.
   */
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

  @Override
  public byte[] getSeed() {
    lock.lock();
    try {
      return seed.clone();
    } finally {
      lock.unlock();
    }
  }

  @SuppressWarnings("method.invocation.invalid")
  @EnsuresNonNull("this.seed")
  @Override
  public synchronized void setSeed(@UnknownInitialization(Random.class)BaseRandom this,
      final long seed) {
    if (superConstructorFinished) {
      assert longSeedArray != null : "@AssumeAssertion(nullness)";
      assert longSeedBuffer != null : "@AssumeAssertion(nullness)";
      longSeedBuffer.putLong(seed);
      setSeed(longSeedArray);
    } else {
      setSeedInternal(BinaryUtils.convertLongToBytes(seed));
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

  /**
   * <p>addSubSubclassFields.</p>
   *
   * @param original a {@link ToStringHelper} object.
   * @return a {@link ToStringHelper} object.
   */
  protected abstract ToStringHelper addSubSubclassFields(ToStringHelper original);

  /**
   * <p>Setter for the field {@code seederThread}.</p>
   *
   * @param thread a {@link io.github.pr0methean.betterrandom.seed.RandomSeederThread} object.
   */
  @SuppressWarnings("ObjectEquality")
  public void setSeederThread(@org.jetbrains.annotations.Nullable final RandomSeederThread thread) {
    if (thread != null) {
      thread.add(this);
    }
    lock.lock();
    try {
      if (this.seederThread == thread) {
        return;
      }
      if (this.seederThread != null) {
        this.seederThread.remove(this);
      }
      this.seederThread = thread;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean preferSeedWithLong() {
    return getNewSeedLength() <= 8;
  }

  /**
   * Sets the seed, and should be overridden to set other state that derives from the seed. Called
   * by {@link #setSeed(byte[])}, whose default implementation ensures that the lock is held while
   * doing so. Also called by constructors, {@link #readObject(ObjectInputStream)} and {@link
   * #readObjectNoData()}.
   *
   * @param seed The new seed.
   */
  @EnsuresNonNull({"this.seed", "entropyBits"})
  protected void setSeedInternal(@UnknownInitialization(Random.class)BaseRandom this,
      final byte[] seed) {
    if (this.seed == null) {
      this.seed = seed.clone();
    } else {
      System.arraycopy(seed, 0, this.seed, 0, seed.length);
    }
    if (entropyBits == null) {
      entropyBits = new AtomicLong(0);
    }
    entropyBits.updateAndGet(
        oldCount -> Math.max(oldCount, Math.min(seed.length, getNewSeedLength()) * 8));
  }

  /**
   * Called in constructor and readObject to initialize transient fields.
   */
  @EnsuresNonNull({"lock", "longSeedArray", "longSeedBuffer"})
  protected void initTransientFields(@UnknownInitialization BaseRandom this) {
    if (lock == null) {
      lock = new ReentrantLock();
    }
    longSeedArray = new byte[8];
    longSeedBuffer = ByteBuffer.wrap(longSeedArray);
    superConstructorFinished = true;
  }

  @EnsuresNonNull({"lock", "seed", "entropyBits"})
  private void readObject(@UnderInitialization(BaseRandom.class)BaseRandom this,
      final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    initTransientFields();
    setSeedInternal(castNonNull(seed));
  }

  @Override
  public long entropyBits() {
    return entropyBits.get();
  }

  /**
   * Record that entropy has been spent, and schedule a reseeding if this PRNG has now spent as much
   * as it's been seeded with.
   *
   * @param bits The number of bits of entropy spent.
   */
  protected void recordEntropySpent(final long bits) {
    if (entropyBits.updateAndGet(oldCount -> Math.max(oldCount - bits, 0)) == 0
        && seederThread != null) {
      seederThread.asyncReseed(this);
    }
  }

  /**
   * Used to deserialize a subclass instance that wasn't a subclass instance when it was serialized.
   * Since that means we can't deserialize our seed, we generate a new one with the {@link
   * DefaultSeedGenerator}.
   *
   * @throws InvalidObjectException if the {@link DefaultSeedGenerator} fails.
   */
  @EnsuresNonNull({"lock", "seed", "entropyBits"})
  @SuppressWarnings("OverriddenMethodCallDuringObjectConstruction")
  private void readObjectNoData() throws InvalidObjectException {
    LOG.warn("BaseRandom.readObjectNoData() invoked; using DefaultSeedGenerator");
    fallbackSetSeed();
    initTransientFields();
    setSeedInternal(this.seed);
  }

  /**
   * Generates a seed using the default seed generator. For use in handling a {@link #setSeed(long)}
   * call in subclasses that can't actually use an 8-byte seed.
   */
  @EnsuresNonNull("seed")
  protected void fallbackSetSeed(@UnknownInitialization BaseRandom this) {
    try {
      this.seed = DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(getNewSeedLength());
    } catch (final SeedException e) {
      throw new RuntimeException(e);
    }
  }

  protected void recordAllEntropySpent() {
    entropyBits.set(0);
    if (seederThread != null) {
      seederThread.asyncReseed(this);
    }
  }

  @Override
  public abstract int getNewSeedLength(@UnknownInitialization BaseRandom this);
}
