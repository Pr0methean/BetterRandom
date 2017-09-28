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
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.DoubleSupplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Abstract {@link Random} with a seed field and an implementations of entropy counting.
 *
 * @author Chris Hennick
 */
public abstract class BaseRandom extends Random implements ByteArrayReseedableRandom,
    RepeatableRandom, Dumpable, EntropyCountingRandom {

  public static final int ENTROPY_OF_DOUBLE = 53;
  public static final long NAN_LONG_BITS = Double.doubleToLongBits(Double.NaN);
  protected static final long ENTROPY_OF_FLOAT = 24;
  private static final LogPreFormatter LOG = new LogPreFormatter(BaseRandom.class);
  private static final long serialVersionUID = -1556392727255964947L;
  protected byte[] seed;
  /** Lock to prevent concurrent modification of the RNG's internal state. */
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
  protected AtomicReference<@Nullable RandomSeederThread> seederThread = new AtomicReference<>(
      null);
  private AtomicLong nextNextGaussian = new AtomicLong(
      NAN_LONG_BITS); // Stored as a long since there's no atomic double

  /**
   * Creates a new RNG and seeds it using the default seeding strategy.
   *
   * @param seedLength a int.
   * @throws SeedException if any.
   */
  public BaseRandom(final int seedLength) throws SeedException {
    this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(seedLength));
    entropyBits = new AtomicLong(0);
  }

  /**
   * Creates a new RNG and seeds it using the provided seed generation strategy.
   *
   * @param seedGenerator The seed generation strategy that will provide the seed value for this
   *     RNG.
   * @param seedLength The seed length in bytes.
   * @throws SeedException If there is a problem generating a seed.
   */
  public BaseRandom(final SeedGenerator seedGenerator, final int seedLength) throws SeedException {
    this(seedGenerator.generateSeed(seedLength));
    entropyBits = new AtomicLong(0);
  }

  /**
   * Creates a new RNG with the provided seed.
   *
   * @param seed the seed.
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
   * Creates a new RNG with the provided seed. Only works in subclasses that can accept an 8-byte or
   * shorter seed.
   *
   * @param seed the seed.
   */
  protected BaseRandom(long seed) {
    this(BinaryUtils.convertLongToBytes(seed));
  }

  /**
   * @param origin the minimum, inclusive.
   * @param bound the maximum, exclusive.
   * @return the entropy in bits, rounded up, of a random {@code int} between {@code origin} and
   *     {@code bound}.
   */
  protected static long entropyOfInt(final int origin, final int bound) {
    return Integer.SIZE - Integer.numberOfLeadingZeros(bound - origin - 1);
  }

  /**
   * @param origin the minimum, inclusive.
   * @param bound the maximum, exclusive.
   * @return the entropy in bits, rounded up, of a random {@code long} between {@code origin} and
   *     {@code bound}.
   */
  protected static long entropyOfLong(final long origin, final long bound) {
    return Long.SIZE - Long.numberOfLeadingZeros(bound - origin - 1);
  }

  /**
   * The purpose of this method is so that entropy-counting subclasses can detect that no more than
   * 1 bit of entropy is actually being consumed, even though this method uses {@link #nextDouble()}
   * which normally consumes 53 bits. Tracking of fractional bits of entropy is currently not
   * implemented in BaseRandom.
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
    final boolean result = super.nextDouble() < probability;
    // We're only outputting one bit
    recordEntropySpent(1);
    return result;
  }

  public final ToStringHelper addSubclassFields(final ToStringHelper original) {
    return addSubSubclassFields(original
        .add("entropyBits", entropyBits.get())
        .add("seederThread", seederThread));
  }

  /** {@inheritDoc} Reimplemented for entropy-counting purposes. */
  @SuppressWarnings("NumericCastThatLosesPrecision")
  @Override
  public void nextBytes(byte[] bytes) {
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) next(Byte.SIZE);
      recordEntropySpent(Byte.SIZE);
    }
  }

  @Override
  public int nextInt() {
    recordEntropySpent(Integer.SIZE);
    return super.nextInt();
  }

  @Override
  public int nextInt(int bound) {
    recordEntropySpent(entropyOfInt(0, bound));
    return super.nextInt(bound);
  }

  @Override
  public long nextLong() {
    recordEntropySpent(Long.SIZE);
    return super.nextLong();
  }

  @Override
  public boolean nextBoolean() {
    recordEntropySpent(1);
    return super.nextBoolean();
  }

  @Override
  public float nextFloat() {
    recordEntropySpent(ENTROPY_OF_FLOAT);
    return super.nextFloat();
  }

  @Override
  public double nextDouble() {
    recordEntropySpent(ENTROPY_OF_DOUBLE);
    return super.nextDouble();
  }

  /**
   * {@inheritDoc} This is overridden both for entropy-counting purposes and to make it lockless.
   */
  @Override
  public double nextGaussian() {
    // Upper bound. 2 Gaussians are generated from 2 nextDouble calls, which once made are either
    // used or rerolled.
    recordEntropySpent(ENTROPY_OF_DOUBLE);
    return internalNextGaussian(super::nextDouble);
  }

  /**
   * Core of a lockless reimplementation of {@link #nextGaussian()}.
   *
   * @param nextDouble shall return a random number between 0 and 1, like {@link #nextDouble()},
   *     but shall not debit the entropy count.
   * @return a random number that is normally distributed with mean 0 and standard deviation 1.
   */
  protected double internalNextGaussian(DoubleSupplier nextDouble) {
    // See Knuth, ACP, Section 3.4.1 Algorithm C.
    double out = Double.longBitsToDouble(nextNextGaussian.getAndSet(NAN_LONG_BITS));
    if (Double.isNaN(out)) {
      double v1, v2, s;
      do {
        v1 = 2 * nextDouble.getAsDouble() - 1; // between -1 and 1
        v2 = 2 * nextDouble.getAsDouble() - 1; // between -1 and 1
        s = v1 * v1 + v2 * v2;
      } while (s >= 1 || s == 0);
      double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
      this.nextNextGaussian.set(Double.doubleToRawLongBits(v2 * multiplier));
      return v1 * multiplier;
    } else {
      return out;
    }
  }

  @Override
  public IntStream ints(long streamSize) {
    recordEntropySpent(Integer.SIZE * streamSize);
    return super.ints(streamSize);
  }

  @Override
  public IntStream ints() {
    recordAllEntropySpent();
    return super.ints();
  }

  @Override
  public IntStream ints(long streamSize, int randomNumberOrigin, int randomNumberBound) {
    recordEntropySpent(streamSize * entropyOfInt(randomNumberOrigin, randomNumberBound));
    return super.ints(streamSize, randomNumberOrigin, randomNumberBound);
  }

  @Override
  public IntStream ints(int randomNumberOrigin, int randomNumberBound) {
    recordAllEntropySpent();
    return super.ints(randomNumberOrigin, randomNumberBound);
  }

  @Override
  public LongStream longs(long streamSize) {
    recordEntropySpent(streamSize * Long.SIZE);
    return super.longs(streamSize);
  }

  @Override
  public LongStream longs() {
    recordAllEntropySpent();
    return super.longs();
  }

  @Override
  public LongStream longs(long streamSize, long randomNumberOrigin, long randomNumberBound) {
    recordEntropySpent(streamSize * entropyOfLong(randomNumberOrigin, randomNumberBound));
    return super.longs(streamSize, randomNumberOrigin, randomNumberBound);
  }

  @Override
  public LongStream longs(long randomNumberOrigin, long randomNumberBound) {
    recordAllEntropySpent();
    return super.longs(randomNumberOrigin, randomNumberBound);
  }

  @Override
  public DoubleStream doubles(long streamSize) {
    recordEntropySpent(streamSize * ENTROPY_OF_DOUBLE);
    return super.doubles(streamSize);
  }

  @Override
  public DoubleStream doubles() {
    recordAllEntropySpent();
    return super.doubles();
  }

  @Override
  public DoubleStream doubles(long streamSize, double randomNumberOrigin,
      double randomNumberBound) {
    recordEntropySpent(streamSize * ENTROPY_OF_DOUBLE);
    return super.doubles(streamSize, randomNumberOrigin, randomNumberBound);
  }

  @Override
  public DoubleStream doubles(double randomNumberOrigin, double randomNumberBound) {
    recordAllEntropySpent();
    return super.doubles(randomNumberOrigin, randomNumberBound);
  }

  @Override
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

  @SuppressWarnings("method.invocation.invalid")
  @EnsuresNonNull("this.seed")
  @Override
  public synchronized void setSeed(@UnknownInitialization(Random.class)BaseRandom this,
      final long seed) {
    if (superConstructorFinished) {
      castNonNull(longSeedBuffer).putLong(0, seed);
      setSeed(castNonNull(longSeedArray));
    } else {
      setSeedInternal(BinaryUtils.convertLongToBytes(seed));
    }
  }

  /**
   * Adds the fields that were not inherited from {@link BaseRandom} to the given {@link
   * ToStringHelper} for dumping.
   *
   * @param original a {@link ToStringHelper} object.
   * @return {@code original} with the fields not inherited from {@link BaseRandom} written to it.
   */
  protected abstract ToStringHelper addSubSubclassFields(ToStringHelper original);

  /**
   * Registers this PRNG with the given {@link RandomSeederThread} to schedule reseeding when we run
   * out of entropy. Unregisters
   *
   * @param thread a {@link RandomSeederThread} that will be used to reseed this PRNG.
   */
  @SuppressWarnings("ObjectEquality")
  public void setSeederThread(final @Nullable RandomSeederThread thread) {
    if (seederThread.getAndSet(thread) != thread && thread != null) {
      thread.add(this);
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
        oldCount -> Math.max(oldCount, Math.min(seed.length, getNewSeedLength()) * 8L));
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
    if (entropyBits.addAndGet(-bits) <= 0) {
      RandomSeederThread thread = seederThread.get();
      if (thread != null) {
        thread.asyncReseed(this);
      }
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
    try {
      fallbackSetSeed();
    } catch (RuntimeException e) {
      throw (InvalidObjectException) (new InvalidObjectException(
          "Failed to deserialize or generate a seed")
          .initCause(e.getCause()));
    }
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
