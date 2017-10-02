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
import io.github.pr0methean.betterrandom.util.IntSupplierSpliterator;
import io.github.pr0methean.betterrandom.util.LogPreFormatter;
import io.github.pr0methean.betterrandom.util.LongSupplierSpliterator;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.DoubleSupplier;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;
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

  /** The number of pseudorandom bits in {@link #nextFloat()}(). */
  protected static final long ENTROPY_OF_FLOAT = 24;

  /** The number of pseudorandom bits in {@link #nextDouble()}(). */
  protected static final int ENTROPY_OF_DOUBLE = 53;

  private static final long NAN_LONG_BITS = Double.doubleToLongBits(Double.NaN);
  private static final LogPreFormatter LOG = new LogPreFormatter(BaseRandom.class);
  private static final long serialVersionUID = -1556392727255964947L;
  /**
   * If the referent is non-null, it will be invoked to reseed this PRNG whenever random output is
   * taken and {@link #getEntropyBits()} called immediately afterward would return zero or
   * negative.
   */
  protected final AtomicReference<@Nullable RandomSeederThread> seederThread = new AtomicReference<>(
      null);
  private final AtomicLong nextNextGaussian = new AtomicLong(
      NAN_LONG_BITS); // Stored as a long since there's no atomic double
  /**
   * The seed this PRNG was seeded with, as a byte array. Used by {@link #getSeed()} even if the
   * actual internal state of the PRNG is stored elsewhere (since otherwise getSeed() would require
   * a slow type conversion).
   */
  protected byte[] seed;
  /** Lock to prevent concurrent modification of the RNG's internal state. */
  @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
  protected transient Lock lock;
  /**
   * Set by the constructor once either {@link Random#Random()} or {@link Random#Random(long)}.
   * Intended for {@link #setSeed(long)}, which may have to ignore calls while this is false if the
   * subclass does not support 8-byte seeds, or it overriddes setSeed(long) to use subclass fields.
   */
  @SuppressWarnings({"InstanceVariableMayNotBeInitializedByReadObject",
      "FieldAccessedSynchronizedAndUnsynchronized"})
  protected transient boolean superConstructorFinished = false;
  /** Stores the entropy estimate backing {@link #getEntropyBits()}. */
  protected AtomicLong entropyBits;

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
  protected BaseRandom(final long seed) {
    this(BinaryUtils.convertLongToBytes(seed));
  }

  /**
   * Calculates the entropy in bits, rounded up, of a random {@code int} between {@code origin}
   * (inclusive) and {@code bound} (exclusive).
   *
   * @param origin the minimum, inclusive.
   * @param bound the maximum, exclusive.
   * @return the entropy.
   */
  protected static long entropyOfInt(final int origin, final int bound) {
    return Integer.SIZE - Integer.numberOfLeadingZeros(bound - origin - 1);
  }

  /**
   * Calculates the entropy in bits, rounded up, of a random {@code long} between {@code origin}
   * (inclusive) and {@code bound} (exclusive).
   *
   * @param origin the minimum, inclusive.
   * @param bound the maximum, exclusive.
   * @return the entropy.
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
    return (probability >= 1) || ((probability > 0) && withProbabilityInternal(probability));
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

  /**
   * Chooses a random element from the given array.
   *
   * @param array A non-empty array to choose from.
   * @param <E> The element type of {@code array}; usually inferred by the compiler.
   * @return An element chosen from {@code array} at random, with all elements having equal
   *     probability.
   */
  public <E> E nextElement(final E[] array) {
    return array[nextInt(array.length)];
  }

  /**
   * Chooses a random element from the given list.
   *
   * @param list A non-empty {@link List} to choose from.
   * @param <E> The element type of {@code list}; usually inferred by the compiler.
   * @return An element chosen from {@code list} at random, with all elements having equal
   *     probability.
   */
  public <E> E nextElement(final List<E> list) {
    return list.get(nextInt(list.size()));
  }

  /**
   * Chooses a random value of the given enum class.
   *
   * @param enumClass An enum class having at least one value.
   * @param <E> The type of {@code enumClass}; usually inferred by the compiler.
   * @return A value of {@code enumClass} chosen at random, with all elements having equal
   *     probability.
   */
  public <E extends Enum<E>> E nextEnum(final Class<E> enumClass) {
    return nextElement(enumClass.getEnumConstants());
  }

  /**
   * Generates the next pseudorandom number. Called by all other random-number-generating methods.
   * Should not debit the entropy count, since that's done by the calling methods according to the
   * amount they actually output (see for example {@link #withProbability(double)}, which uses 53
   * random bits but outputs only one, and thus debits only 1 bit of entropy).
   */
  @Override
  protected abstract int next(int bits);

  /** {@inheritDoc} Reimplemented for entropy-counting purposes. */
  @SuppressWarnings("NumericCastThatLosesPrecision")
  @Override
  public void nextBytes(final byte[] bytes) {
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
  public int nextInt(final int bound) {
    recordEntropySpent(entropyOfInt(0, bound));
    return super.nextInt(bound);
  }

  @Override
  public final long nextLong() {
    recordEntropySpent(Long.SIZE);
    return nextLongNoEntropyDebit();
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
  protected double internalNextGaussian(final DoubleSupplier nextDouble) {
    // See Knuth, ACP, Section 3.4.1 Algorithm C.
    final double out = Double.longBitsToDouble(nextNextGaussian.getAndSet(NAN_LONG_BITS));
    if (Double.isNaN(out)) {
      double v1, v2, s;
      do {
        v1 = (2 * nextDouble.getAsDouble()) - 1; // between -1 and 1
        v2 = (2 * nextDouble.getAsDouble()) - 1; // between -1 and 1
        s = (v1 * v1) + (v2 * v2);
      } while ((s >= 1) || (s == 0));
      final double multiplier = StrictMath.sqrt((-2 * StrictMath.log(s)) / s);
      nextNextGaussian.set(Double.doubleToRawLongBits(v2 * multiplier));
      return v1 * multiplier;
    } else {
      return out;
    }
  }

  @Override
  public IntStream ints(final long streamSize) {
    return StreamSupport.intStream(new IntSupplierSpliterator(streamSize, this::nextInt),
        true);
  }

  @Override
  public IntStream ints() {
    return ints(Long.MAX_VALUE);
  }

  @Override
  public IntStream ints(final long streamSize, final int randomNumberOrigin,
      final int randomNumberBound) {
    return StreamSupport.intStream(new IntSupplierSpliterator(streamSize,
            () -> nextInt(randomNumberOrigin, randomNumberBound)),
        true);
  }

  /**
   * Returns a pseudorandom {@code int} value between the specified origin (inclusive) and the
   * specified bound (exclusive).
   *
   * @param origin the least value returned
   * @param bound the upper bound (exclusive)
   * @return a pseudorandom {@code int} value between the origin (inclusive) and the bound
   *     (exclusive)
   * @throws IllegalArgumentException if {@code origin} is greater than or equal to {@code
   *     bound}
   */
  public int nextInt(final int origin, final int bound) {
    if (bound <= origin) {
      throw new IllegalArgumentException(String.format("Bound %d must be greater than origin %d",
          bound, origin));
    }
    final int range = bound - origin;
    if (range > 0) {
      // range is no more than Integer.MAX_VALUE
      return nextInt(range) + origin;
    } else {
      int output;
      do {
        output = super.nextInt();
      } while ((output < origin) || (output >= bound));
      recordEntropySpent(entropyOfInt(origin, bound));
      return output;
    }
  }

  @Override
  public IntStream ints(final int randomNumberOrigin, final int randomNumberBound) {
    return ints(Long.MAX_VALUE, randomNumberOrigin, randomNumberBound);
  }

  @Override
  public LongStream longs(final long streamSize) {
    return StreamSupport.longStream(new LongSupplierSpliterator(streamSize, this::nextLong),
        true);
  }

  @Override
  public LongStream longs() {
    return longs(Long.MAX_VALUE);
  }

  @Override
  public LongStream longs(final long streamSize, final long randomNumberOrigin,
      final long randomNumberBound) {
    return StreamSupport.longStream(new LongSupplierSpliterator(streamSize,
            () -> nextLong(randomNumberOrigin, randomNumberBound)),
        true);
  }

  /**
   * Returns a pseudorandom {@code long} value between the specified origin (inclusive) and the
   * specified bound (exclusive).
   *
   * @param origin the least value returned
   * @param bound the upper bound (exclusive)
   * @return a pseudorandom {@code long} value between the origin (inclusive) and the bound
   *     (exclusive)
   * @throws IllegalArgumentException if {@code origin} is greater than or equal to {@code
   *     bound}
   */
  public long nextLong(final long origin, final long bound) {
    if (bound <= origin) {
      throw new IllegalArgumentException(String.format("Bound %d must be greater than origin %d",
          bound, origin));
    }
    long output;
    do {
      output = nextLongNoEntropyDebit();
    } while ((output < origin) || (output >= bound));
    recordEntropySpent(entropyOfLong(origin, bound));
    return output;
  }

  /**
   * Returns the next random {@code long}, but does not debit entropy.
   *
   * @return a pseudorandom {@code long} with all possible values equally likely.
   */
  protected long nextLongNoEntropyDebit() {
    return super.nextLong();
  }

  @Override
  public LongStream longs(final long randomNumberOrigin, final long randomNumberBound) {
    return longs(Long.MAX_VALUE, randomNumberOrigin, randomNumberBound);
  }

  @Override
  public String dump() {
    lock.lock();
    try {
      return addSubSubclassFields(MoreObjects.toStringHelper(this)
          .add("seed", BinaryUtils.convertBytesToHexString(seed))
          .add("entropyBits", entropyBits.get())
          .add("seederThread", seederThread))
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

  @EnsuresNonNull({"this.seed", "entropyBits"})
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
  @EnsuresNonNull({"this.seed", "entropyBits"})
  @Override
  public synchronized void setSeed(@UnknownInitialization(Random.class)BaseRandom this,
      final long seed) {
    final byte[] seedBytes = BinaryUtils.convertLongToBytes(seed);
    if (superConstructorFinished) {
      setSeed(seedBytes);
    } else {
      setSeedInternal(seedBytes);
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
   * out of entropy. Unregisters this PRNG with the previous {@link RandomSeederThread} if it had a
   * different one.
   *
   * @param thread a {@link RandomSeederThread} that will be used to reseed this PRNG.
   */
  @SuppressWarnings({"ObjectEquality", "EqualityOperatorComparesObjects"})
  public void setSeederThread(final @Nullable RandomSeederThread thread) {
    RandomSeederThread oldThread = seederThread.getAndSet(thread);
    if (thread != oldThread) {
      if (oldThread != null) {
        oldThread.remove(this);
      }
      if (thread != null) {
        thread.add(this);
      }
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
  @EnsuresNonNull("lock")
  protected void initTransientFields(@UnknownInitialization BaseRandom this) {
    if (lock == null) {
      lock = new ReentrantLock();
    }
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
  public long getEntropyBits() {
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
      asyncReseedIfPossible();
    }
  }

  private void asyncReseedIfPossible() {
    final RandomSeederThread thread = seederThread.get();
    if (thread != null) {
      thread.asyncReseed(this);
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
    } catch (final RuntimeException e) {
      throw (InvalidObjectException) (new InvalidObjectException(
          "Failed to deserialize or generate a seed")
          .initCause(e.getCause()));
    }
    initTransientFields();
    setSeedInternal(seed);
  }

  /**
   * Generates a seed using the default seed generator if there isn't one already. For use in
   * handling a {@link #setSeed(long)} call in subclasses that can't actually use an 8-byte seed.
   */
  @SuppressWarnings("LockAcquiredButNotSafelyReleased")
  @EnsuresNonNull({"seed", "entropyBits"})
  protected void fallbackSetSeed(@UnknownInitialization BaseRandom this) {
    boolean locked = false;
    if (lock != null) {
      lock.lock();
      locked = true;
    }
    try {
      if (seed == null) {
        try {
          seed = DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(getNewSeedLength());
        } catch (final SeedException e) {
          throw new RuntimeException(e);
        }
      }
      if (entropyBits == null) {
        entropyBits = new AtomicLong(seed.length * 8);
      }
    } finally {
      if (locked) {
        lock.unlock();
      }
    }
  }

  @Override
  public abstract int getNewSeedLength(@UnknownInitialization BaseRandom this);
}
