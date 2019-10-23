package io.github.pr0methean.betterrandom.prng;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import io.github.pr0methean.betterrandom.EntropyCountingRandom;
import io.github.pr0methean.betterrandom.RepeatableRandom;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeeder;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.Dumpable;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import io.github.pr0methean.betterrandom.util.Java8Constants;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java8.util.function.DoubleSupplier;
import java8.util.function.IntSupplier;
import java8.util.function.LongSupplier;
import java8.util.function.LongToDoubleFunction;
import java8.util.function.LongToIntFunction;
import java8.util.function.LongUnaryOperator;
import java8.util.stream.BaseStream;
import java8.util.stream.DoubleStream;
import java8.util.stream.DoubleStreams;
import java8.util.stream.IntStream;
import java8.util.stream.IntStreams;
import java8.util.stream.LongStream;
import java8.util.stream.LongStreams;
import javax.annotation.Nullable;

/**
 * Abstract {@link Random} with a seed field and an implementation of entropy counting.
 *
 * @author Chris Hennick
 */
public abstract class BaseRandom extends Random
    implements ByteArrayReseedableRandom, RepeatableRandom, Dumpable, EntropyCountingRandom,
    Java8CompatRandom {

  /**
   * The number of pseudorandom bits in {@link #nextFloat()}.
   */
  protected static final int ENTROPY_OF_FLOAT = 24;

  /**
   * The number of pseudorandom bits in {@link #nextDouble()}.
   */
  protected static final int ENTROPY_OF_DOUBLE = 53;

  private static final long NAN_LONG_BITS = Double.doubleToRawLongBits(Double.NaN);
  private static final long serialVersionUID = -1556392727255964947L;
  /**
   * If the referent is non-null, it will be invoked to reseed this PRNG whenever random output is
   * taken and {@link #getEntropyBits()} called immediately afterward would return zero or
   * negative.
   */
  protected final AtomicReference<SimpleRandomSeeder> randomSeeder = new AtomicReference<>(null);
  /**
   * Lock to prevent concurrent modification of the RNG's internal state.
   */
  protected final ReentrantLock lock = new ReentrantLock();
  /**
   * Stores the entropy estimate backing {@link #getEntropyBits()}.
   */
  protected final AtomicLong entropyBits = new AtomicLong(0);
  // Stored as a long since there's no atomic double
  private final AtomicLong nextNextGaussian = new AtomicLong(NAN_LONG_BITS);
  /**
   * The seed this PRNG was seeded with, as a byte array. Used by {@link #getSeed()} even if the
   * actual internal state of the PRNG is stored elsewhere (since otherwise getSeed() would require
   * a slow type conversion).
   */
  protected volatile byte[] seed;
  /**
   * Set by the constructor once either {@link Random#Random()} or {@link Random#Random(long)} has
   * returned. Intended for {@link #setSeed(long)}, which may have to ignore calls while this is
   * false if the subclass does not support 8-byte seeds, or it overriddes setSeed(long) to use
   * subclass fields.
   */
  @SuppressWarnings({"InstanceVariableMayNotBeInitializedByReadObject"}) protected transient boolean
      superConstructorFinished = false;

  /**
   * Seed the RNG using the {@link DefaultSeedGenerator} to create a seed of the specified size.
   *
   * @param seedSizeBytes The number of bytes to use for seed data.
   * @throws SeedException if the {@link DefaultSeedGenerator} fails to generate a seed.
   */
  protected BaseRandom(final int seedSizeBytes) throws SeedException {
    this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(seedSizeBytes));
  }

  /**
   * Creates a new RNG and seeds it using the provided seed generation strategy.
   *
   * @param randomSeeder The seed generation strategy that will provide the seed value for this
   *     RNG.
   * @param seedLength The seed length in bytes.
   * @throws SeedException If there is a problem generating a seed.
   */
  protected BaseRandom(final SeedGenerator randomSeeder, final int seedLength)
      throws SeedException {
    this(randomSeeder.generateSeed(seedLength));
  }

  /**
   * Creates a new RNG with the provided seed.
   *
   * @param seed the seed.
   */
  protected BaseRandom(final byte[] seed) {
    if (seed == null) {
      throw new IllegalArgumentException("Seed must not be null");
    }
    initTransientFields();
    setSeedInternal(seed);
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
  protected static int entropyOfInt(final int origin, final int bound) {
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
  protected static int entropyOfLong(final long origin, final long bound) {
    return Long.SIZE - Long.numberOfLeadingZeros(bound - origin - 1);
  }

  /**
   * @return true if this PRNG creates parallel streams; false otherwise.
   */
  public boolean usesParallelStreams() {
    return false;
  }

  /**
   * <p>Returns true with the given probability, and records that only 1 bit of entropy is being
   * spent.</p> <p>When {@code probability <= 0}, instantly returns false without recording any
   * entropy spent. Likewise, instantly returns true when {@code probability >= 1}.</p>
   *
   * @param probability The probability of returning true.
   * @return True with probability equal to the {@code probability} parameter; false otherwise.
   */
  @SuppressWarnings("FloatingPointEquality") public boolean withProbability(
      final double probability) {
    if (probability >= 1) {
      return true;
    }
    if (probability <= 0) {
      return false;
    }
    if (probability == 0.5) {
      return nextBoolean();
    }
    return withProbabilityInternal(probability);
  }

  /**
   * Called by {@link #withProbability(double)} to generate a boolean with a specified probability
   * of returning true, after checking that {@code probability} is strictly between 0 and 1.
   *
   * @param probability The probability (between 0 and 1 exclusive) of returning true.
   * @return True with probability equal to the {@code probability} parameter; false otherwise.
   */
  protected boolean withProbabilityInternal(final double probability) {
    final boolean result = super.nextDouble() < probability;
    // We're only outputting one bit
    debitEntropy(1);
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
  @Override protected abstract int next(int bits);

  /**
   * Generates random bytes and places them into a user-supplied byte array. The number of random
   * bytes produced is equal to the length of the byte array. Reimplemented for entropy-counting
   * purposes.
   */
  @SuppressWarnings("NumericCastThatLosesPrecision") @Override public void nextBytes(
      final byte[] bytes) {
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) next(Byte.SIZE);
      debitEntropy(Byte.SIZE);
    }
  }

  @Override public int nextInt() {
    debitEntropy(Integer.SIZE);
    return super.nextInt();
  }

  @Override public int nextInt(final int bound) {
    debitEntropy(entropyOfInt(0, bound));
    return super.nextInt(bound);
  }

  /**
   * Returns the next pseudorandom, uniformly distributed long value from this random number
   * generator's sequence. Unlike the inherited implementation in {@link Random#nextLong()}, ones in
   * BetterRandom generally <i>can</i> be expected to return all 2<sup>64</sup> possible values.
   */
  @Override public long nextLong() {
    final long out = nextLongNoEntropyDebit();
    debitEntropy(Long.SIZE);
    return out;
  }

  /**
   * Returns a pseudorandom {@code long} value between zero (inclusive) and the specified bound
   * (exclusive).
   *
   * @param bound the upper bound (exclusive).  Must be positive.
   * @return a pseudorandom {@code long} value between zero (inclusive) and the bound (exclusive)
   * @throws IllegalArgumentException if {@code bound} is not positive
   */
  public long nextLong(final long bound) {
    return nextLong(0, bound);
  }

  /**
   * Returns a pseudorandom {@code double} value between 0.0 (inclusive) and the specified bound
   * (exclusive).
   *
   * @param bound the upper bound (exclusive).  Must be positive.
   * @return a pseudorandom {@code double} value between zero (inclusive) and the bound (exclusive)
   * @throws IllegalArgumentException if {@code bound} is not positive
   */
  @EntryPoint public double nextDouble(final double bound) {
    return nextDouble(0.0, bound);
  }

  /**
   * Returns a pseudorandom {@code double} value between the specified origin (inclusive) and bound
   * (exclusive).
   *
   * @param origin the least value returned
   * @param bound the upper bound (exclusive)
   * @return a pseudorandom {@code double} value between the origin (inclusive) and the bound
   *     (exclusive)
   * @throws IllegalArgumentException if {@code origin} is greater than or equal to {@code
   *     bound}
   */
  public double nextDouble(final double origin, final double bound) {
    if (bound <= origin) {
      throw new IllegalArgumentException(
          String.format("Bound %f must be greater than origin %f", bound, origin));
    }
    final double out = (nextDouble() * (bound - origin)) + origin;
    if (out >= bound) {
      // correct for rounding
      return Double.longBitsToDouble(Double.doubleToRawLongBits(bound) - 1);
    }
    return out;
  }

  private <T extends BaseStream<?, T>> T maybeParallel(final T in) {
    return usesParallelStreams() ? in.parallel() : in;
  }

  /**
   * <p>Returns a stream producing an effectively unlimited number of pseudorandom doubles, each
   * conforming to the given origin (inclusive) and bound (exclusive). This implementation uses
   * {@link #nextDouble(double, double)} to generate these numbers.</p>
   */
  @Override public DoubleStream doubles(final double randomNumberOrigin,
      final double randomNumberBound) {
    return maybeParallel(DoubleStreams.generate(new DoubleSupplier() {
      @Override public double getAsDouble() {
        return nextDouble(randomNumberOrigin, randomNumberBound);
      }
    }));
  }

  /**
   * <p>Returns a stream producing an effectively unlimited number of pseudorandom doubles, each
   * between 0.0 (inclusive) and 1.0 (exclusive). This implementation uses {@link #nextDouble()} to
   * generate these numbers.</p>
   */
  @Override public DoubleStream doubles() {
    return maybeParallel(DoubleStreams.generate(new DoubleSupplier() {
      @Override public double getAsDouble() {
        return nextDouble();
      }
    }));
  }

  @Override public DoubleStream doubles(final long streamSize) {
    return streamOfSize(streamSize).mapToDouble(new LongToDoubleFunction() {
      @Override public double applyAsDouble(long l) {
        return nextDouble();
      }
    });
  }

  private LongStream streamOfSize(final long streamSize) {
    return maybeParallel(LongStreams.range(0, streamSize).unordered());
  }

  /**
   * Returns a stream producing the given number of pseudorandom doubles, each conforming to the
   * given origin (inclusive) and bound (exclusive). This implementation uses {@link
   * #nextDouble(double, double)} to generate these numbers.
   */
  @Override public DoubleStream doubles(final long streamSize, final double randomNumberOrigin,
      final double randomNumberBound) {
    return streamOfSize(streamSize).mapToDouble(new LongToDoubleFunction() {
      @Override public double applyAsDouble(long l) {
        return nextDouble(randomNumberOrigin, randomNumberBound);
      }
    });
  }

  /**
   * <p>Returns a stream producing an effectively unlimited number of pseudorandom doubles that are
   * normally distributed with mean 0.0 and standard deviation 1.0. This implementation uses {@link
   * #nextGaussian()}.</p>
   *
   * @return a stream of normally-distributed random doubles.
   */
  public DoubleStream gaussians() {
    return maybeParallel(DoubleStreams.generate(new DoubleSupplier() {
      @Override public double getAsDouble() {
        return nextGaussian();
      }
    }));
  }

  /**
   * Returns a stream producing the given number of pseudorandom doubles that are normally
   * distributed with mean 0.0 and standard deviation 1.0. This implementation uses {@link
   * #nextGaussian()}.
   *
   * @param streamSize the number of doubles to generate.
   * @return a stream of {@code streamSize} normally-distributed random doubles.
   */
  public DoubleStream gaussians(final long streamSize) {
    return streamOfSize(streamSize).mapToDouble(new LongToDoubleFunction() {
      @Override public double applyAsDouble(long l) {
        return nextGaussian();
      }
    });
  }

  @Override public boolean nextBoolean() {
    debitEntropy(1);
    return super.nextBoolean();
  }

  @Override public float nextFloat() {
    debitEntropy(ENTROPY_OF_FLOAT);
    return super.nextFloat();
  }

  @Override public double nextDouble() {
    debitEntropy(ENTROPY_OF_DOUBLE);
    return nextDoubleNoEntropyDebit();
  }

  /**
   * Returns the next random {@code double} between 0.0 (inclusive) and 1.0 (exclusive), but does
   * not debit entropy.
   *
   * @return a pseudorandom {@code double}.
   */
  protected double nextDoubleNoEntropyDebit() {
    lock.lock();
    try {
      return super.nextDouble();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Returns the next pseudorandom, Gaussian ("normally") distributed double value with mean 0.0 and
   * standard deviation 1.0 from this random number generator's sequence. Unlike the one in {@link
   * Random}, this implementation is lockless.
   */
  @SuppressWarnings("NonSynchronizedMethodOverridesSynchronizedMethod") @Override public double nextGaussian() {
    // Upper bound. 2 Gaussians are generated from 2 nextDouble calls, which once made are either
    // used or rerolled.
    debitEntropy(ENTROPY_OF_DOUBLE);
    return internalNextGaussian(new DoubleSupplier() {
      @Override public double getAsDouble() {
        return nextDoubleNoEntropyDebit();
      }
    });
  }

  /**
   * Core of a reimplementation of {@link #nextGaussian()} whose locking is overridable and doesn't
   * happen when a value is already stored.
   *
   * @param nextDouble shall return a random number between 0 and 1, like {@link #nextDouble()},
   *     but shall not debit the entropy count.
   * @return a random number that is normally distributed with mean 0 and standard deviation 1.
   */
  protected double internalNextGaussian(
      final DoubleSupplier nextDouble) {
    // See Knuth, ACP, Section 3.4.1 Algorithm C.
    final double firstTryOut = takeNextNextGaussian();
    if (Double.isNaN(firstTryOut))
      return firstTryOut;
    lockForNextGaussian();
    try {
      // Another output may have become available while we waited for the lock
      final double secondTryOut = takeNextNextGaussian();
      if (!Double.isNaN(secondTryOut)) {
        return secondTryOut;
      }
      double s;
      double v1;
      double v2;
      do {
        v1 = (2 * nextDouble.getAsDouble()) - 1; // between -1 and 1
        v2 = (2 * nextDouble.getAsDouble()) - 1; // between -1 and 1
        s = (v1 * v1) + (v2 * v2);
      } while ((s >= 1) || (s == 0));
      final double multiplier = StrictMath.sqrt((-2 * StrictMath.log(s)) / s);
      nextNextGaussian.set(Double.doubleToRawLongBits(v2 * multiplier));
      return v1 * multiplier;
    } finally {
      unlockForNextGaussian();
    }
  }

  private double takeNextNextGaussian() {
    return Double.longBitsToDouble(nextNextGaussian.getAndSet(NAN_LONG_BITS));
  }

  /**
   * Performs whatever locking is needed by {@link #nextGaussian()}.
   */
  protected void lockForNextGaussian() {
    lock.lock();
  }

  /**
   * Releases the locks acquired by {@link #lockForNextGaussian()}.
   */
  protected void unlockForNextGaussian() {
    lock.unlock();
  }

  @Override public IntStream ints(final long streamSize) {
    return streamOfSize(streamSize).mapToInt(new LongToIntFunction() {
      @Override public int applyAsInt(long l) {
        return nextInt();
      }
    });
  }

  @Override public IntStream ints() {
    return maybeParallel(IntStreams.generate(new IntSupplier() {
      @Override public int getAsInt() {
        return nextInt();
      }
    }));
  }

  /**
   * Returns a stream producing the given number of pseudorandom ints, each conforming to the given
   * origin (inclusive) and bound (exclusive). This implementation uses {@link #nextInt(int, int)}
   * to generate these numbers.
   */
  @Override public IntStream ints(final long streamSize, final int randomNumberOrigin,
      final int randomNumberBound) {
    return streamOfSize(streamSize).mapToInt(new LongToIntFunction() {
      @Override public int applyAsInt(long l) {
        return nextInt(randomNumberOrigin, randomNumberBound);
      }
    });
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
      throw new IllegalArgumentException(
          String.format("Bound %d must be greater than origin %d", bound, origin));
    }
    final int range = bound - origin;
    if (range >= 0) {
      // range is no more than Integer.MAX_VALUE
      return nextInt(range) + origin;
    } else {
      int output;
      do {
        output = super.nextInt();
      } while ((output < origin) || (output >= bound));
      debitEntropy(entropyOfInt(origin, bound));
      return output;
    }
  }

  /**
   * <p>Returns a stream producing an effectively unlimited number of pseudorandom ints, each
   * conforming to the given origin (inclusive) and bound (exclusive). This implementation uses
   * {@link #nextInt(int, int)} to generate these numbers.</p>
   */
  @Override public IntStream ints(final int randomNumberOrigin, final int randomNumberBound) {
    return maybeParallel(IntStreams.generate(new IntSupplier() {
      @Override public int getAsInt() {
        return nextInt(randomNumberOrigin, randomNumberBound);
      }
    }));
  }

  @Override public LongStream longs(final long streamSize) {
    return streamOfSize(streamSize).map(new LongUnaryOperator() {
      @Override public long applyAsLong(long l) {
        return nextLong();
      }
    });
  }

  /**
   * <p>{@inheritDoc}</p> <p>If the returned stream is a parallel stream, consuming it in parallel
   * after calling {@link DoubleStream#limit(long)} may cause extra entropy to be spuriously
   * consumed.</p>
   */
  @Override public LongStream longs() {
    return maybeParallel(LongStreams.generate(new LongSupplier() {
      @Override public long getAsLong() {
        return nextLong();
      }
    }));
  }

  /**
   * <p>Returns a stream producing the given number of pseudorandom longs, each conforming to the
   * given origin (inclusive) and bound (exclusive). This implementation uses
   * {@link #nextLong(long, long)} to generate these numbers.</p>
   */
  @Override public LongStream longs(final long streamSize, final long randomNumberOrigin,
      final long randomNumberBound) {
    return streamOfSize(streamSize).map(new LongUnaryOperator() {
      @Override public long applyAsLong(long l) {
        return nextLong(randomNumberOrigin, randomNumberBound);
      }
    });
  }

  /**
   * Returns a pseudorandom {@code long} value between the specified origin (inclusive) and the
   * specified bound (exclusive). This implementation is adapted from the reference implementation
   * of Random.longs(long, long) from JDK 8.
   *
   * @param origin the least value returned
   * @param bound the upper bound (exclusive)
   * @return a pseudorandom {@code long} value between the origin (inclusive) and the bound
   *     (exclusive)
   * @throws IllegalArgumentException if {@code origin} is greater than or equal to {@code
   *     bound}
   */
  @SuppressWarnings({"StatementWithEmptyBody", "NestedAssignment"}) public long nextLong(
      final long origin, final long bound) {
    if (bound <= origin) {
      throw new IllegalArgumentException(
          String.format("Bound %d must be greater than origin %d", bound, origin));
    }
    lock.lock();
    try {
      long r = nextLongNoEntropyDebit();
      final long n = bound - origin;
      final long m = n - 1;
      if ((n & m) == 0L)  // power of two
      {
        return (r & m) + origin;
      } else if (n > 0L) {  // reject over-represented candidates
        for (long u = r >>> 1;            // ensure nonnegative
            ((u + m) - ((r = u % n))) < 0L;    // rejection check
            u = nextLongNoEntropyDebit() >>> 1) {
        } // retry
        r += origin;
      } else {              // range not representable as long
        while ((r < origin) || (r >= bound)) {
          r = nextLongNoEntropyDebit();
        }
      }
      return r;
    } finally {
      lock.unlock();
      debitEntropy(entropyOfLong(origin, bound));
    }
  }

  /**
   * Returns the next random {@code long}, but does not debit entropy.
   *
   * @return a pseudorandom {@code long} with all possible values equally likely.
   */
  protected long nextLongNoEntropyDebit() {
    lock.lock();
    try {
      return super.nextLong();
    } finally {
      lock.unlock();
    }
  }

  /**
   * <p>Returns a stream producing an effectively unlimited number of pseudorandom longs, each
   * conforming to the given origin (inclusive) and bound (exclusive). This implementation uses
   * {@link #nextLong(long, long)} to generate these numbers.</p>
   */
  @Override public LongStream longs(final long randomNumberOrigin, final long randomNumberBound) {
    return maybeParallel(LongStreams.generate(new LongSupplier() {
      @Override public long getAsLong() {
        return nextLong(randomNumberOrigin, randomNumberBound);
      }
    }));
  }

  @Override public String dump() {
    lock.lock();
    try {
      return addSubclassFields(
          MoreObjects.toStringHelper(this).add("seed", BinaryUtils.convertBytesToHexString(seed))
              .add("entropyBits", entropyBits.get()).add("randomSeeder", randomSeeder)).toString();
    } finally {
      lock.unlock();
    }
  }

  @Override public byte[] getSeed() {
    lock.lock();
    try {
      return seed.clone();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Sets the seed of this random number generator using a single long seed, if this implementation
   * supports that. If it is capable of using 64 bits or less of seed data (i.e. if {@code {@link
   * #getNewSeedLength()} <= {@link Long#BYTES}}), then this method shall replace the entire seed as
   * {@link Random#setSeed(long)} does; otherwise, it shall either combine the input with the
   * existing seed as {@link java.security.SecureRandom#setSeed(long)} does, or it shall generate a
   * new seed using the {@link DefaultSeedGenerator}. The latter is a backward-compatibility measure
   * and can be very slow.
   *
   * @deprecated Some implementations are very slow.
   */
  @Deprecated @SuppressWarnings("NonSynchronizedMethodOverridesSynchronizedMethod") @Override
  public void setSeed(final long seed) {
    final byte[] seedBytes = BinaryUtils.convertLongToBytes(seed);
    if (superConstructorFinished) {
      setSeed(seedBytes);
    } else {
      setSeedInternal(seedBytes);
    }
  }

  /**
   * {@inheritDoc}<p>Most subclasses should override {@link #setSeedInternal(byte[])} instead of
   * this method, so that they will deserialize properly.</p>
   */
  @Override public void setSeed(final byte[] seed) {
    lock.lock();
    try {
      setSeedInternal(seed);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Adds the fields that were not inherited from BaseRandom to the given {@link
   * ToStringHelper} for dumping.
   *
   * @param original a {@link ToStringHelper} object.
   * @return {@code original} with the fields not inherited from BaseRandom written to it.
   */
  protected abstract ToStringHelper addSubclassFields(ToStringHelper original);

  /**
   * Registers this PRNG with the {@link RandomSeederThread} for the corresponding {@link
   * SeedGenerator}, to schedule reseeding when we run out of entropy. Unregisters this PRNG with
   * the previous {@link RandomSeederThread} if it had a different one.
   *
   * @param randomSeeder a {@link SeedGenerator} whose {@link RandomSeederThread} will be used
   *     to reseed this PRNG, or null to stop using one.
   */
  public void setRandomSeeder(@Nullable final SimpleRandomSeeder randomSeeder) {
    SimpleRandomSeeder old = this.randomSeeder.getAndSet(randomSeeder);
    if (old != randomSeeder) {
      if (old != null) {
        old.remove(this);
      }
      if (randomSeeder != null) {
        randomSeeder.add((ByteArrayReseedableRandom) this);
      }
    }
  }

  /**
   * Returns the current seed generator for this PRNG.
   *
   * @return the current seed generator, or null if there is none
   */
  @Nullable public SimpleRandomSeeder getRandomSeeder() {
    return randomSeeder.get();
  }

  @Override public boolean preferSeedWithLong() {
    return getNewSeedLength() <= Java8Constants.LONG_BYTES;
  }

  /**
   * Sets the seed, and should be overridden to set other state that derives from the seed. Called
   * by {@link #setSeed(byte[])}, constructors, and {@code readObject(ObjectInputStream)}. When
   * called after initialization, the {@link #lock} is always held.
   *
   * @param seed The new seed.
   */
  protected void setSeedInternal(final byte[] seed) {
    if (!supportsMultipleSeedLengths()) {
      checkLength(seed, getNewSeedLength());
    }
    if ((this.seed == null) || (this.seed.length != seed.length)) {
      this.seed = seed.clone();
    } else if (seed != this.seed) {
      System.arraycopy(seed, 0, this.seed, 0, seed.length);
    }
    nextNextGaussian.set(NAN_LONG_BITS); // Invalidate Gaussian that was generated from old seed
    creditEntropyForNewSeed(seed.length);
  }

  /**
   * Updates the entropy count to reflect a reseeding. Sets it to the seed length or the internal
   * state size, whichever is shorter, but never less than the existing entropy count.
   *
   * @param seedLength the length of the new seed in bytes
   */
  protected void creditEntropyForNewSeed(int seedLength) {
    final long effectiveBits = Math.min(seedLength, getNewSeedLength()) * 8L;
    long oldCount;
    do {
      oldCount = entropyBits.get();
    } while (!entropyBits.compareAndSet(oldCount, Math.max(oldCount, effectiveBits)));
  }

  /**
   * Called in constructor and readObject to initialize transient fields.
   */
  protected void initTransientFields() {
    superConstructorFinished = true;
  }

  /**
   * Checks that the given seed is the expected length, then returns it.
   *
   * @param seed the seed to check
   * @param requiredLength the expected length
   * @return {@code seed}
   * @throws IllegalArgumentException if {@code seed == null || seed.length != requiredLength}
   */
  protected static byte[] checkLength(final byte[] seed, final int requiredLength) {
    if (seed == null) {
      throw new IllegalArgumentException("Seed must not be null");
    }
    if (seed.length != requiredLength) {
      throw new IllegalArgumentException(
          String.format("Seed length must be %d but got %d", requiredLength, seed.length));
    }
    return seed;
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    initTransientFields();
    setSeedInternal(seed);
    final SimpleRandomSeeder currentSeeder = getRandomSeeder();
    if (currentSeeder != null) {
      currentSeeder.add((ByteArrayReseedableRandom) this);
    }
  }

  @Override public long getEntropyBits() {
    return entropyBits.get();
  }

  /**
   * Record that entropy has been spent, and schedule a reseeding if this PRNG has now spent as much
   * as it's been seeded with.
   *
   * @param bits The number of bits of entropy spent.
   */
  protected void debitEntropy(final long bits) {
    if (entropyBits.addAndGet(-bits) <= 0) {
      asyncReseedIfPossible();
    }
  }

  private void asyncReseedIfPossible() {
    final SimpleRandomSeeder currentSeeder = getRandomSeeder();
    if (currentSeeder != null) {
      currentSeeder.wakeUp();
    }
  }

  /**
   * Do not attempt to deserialize any subclass that wasn't a subclass when serialized.
   */
  private void readObjectNoData()
      throws InvalidObjectException {
    throw new InvalidObjectException(
        "This subclass can't be deserialized, because it wasn't a subclass at serialization time");
  }

  /**
   * Generates and sets a seed using the {@link DefaultSeedGenerator}. Used by {@link #setSeed(long)}
   * in implementations where it can't otherwise fulfill its contract.
   */
  protected void fallbackSetSeedIfInitialized() {
    if (!superConstructorFinished) {
      return;
    }
    lock.lock();
    try {
      setSeedInternal(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(getNewSeedLength()));
    } finally {
      lock.unlock();
    }
  }

  @Override public abstract int getNewSeedLength();

  /**
   * If true, the subclass takes responsibility for checking whether the seed is non-null and has a
   * valid length, and should throw an {@link IllegalArgumentException} in
   * {@link #setSeedInternal(byte[])} if not.
   *
   * @return true if this PRNG supports seed lengths other than {@link #getNewSeedLength()}; false
   *     otherwise.
   */
  protected boolean supportsMultipleSeedLengths() {
    return false;
  }

  @Override public boolean needsReseedingEarly() {
    return false;
  }
}
