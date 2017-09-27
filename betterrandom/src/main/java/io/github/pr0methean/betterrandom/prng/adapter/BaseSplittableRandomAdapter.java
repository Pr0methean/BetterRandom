package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

/**
 * <p>Abstract BaseSplittableRandomAdapter class.</p>
 *
 * @author ubuntu
 * @version $Id: $Id
 */
public abstract class BaseSplittableRandomAdapter extends BaseRandom {

  protected static final int SEED_LENGTH_BYTES = 8;
  private static final long serialVersionUID = 4273652147052638879L;
  private double nextNextGaussian;
  private boolean haveNextNextGaussian;

  /**
   * <p>Constructor for BaseSplittableRandomAdapter.</p>
   *
   * @param seed an array of byte.
   */
  public BaseSplittableRandomAdapter(final byte[] seed) {
    super(seed);
  }

  public BaseSplittableRandomAdapter(long seed) {
    super(seed);
  }

  @Override
  protected void setSeedInternal(
      @UnknownInitialization(Random.class)BaseSplittableRandomAdapter this, byte[] seed) {
    haveNextNextGaussian = false;
    super.setSeedInternal(seed);
  }

  /**
   * <p>getSplittableRandom.</p>
   *
   * @return a {@link java.util.SplittableRandom} object.
   */
  protected abstract SplittableRandom getSplittableRandom();

  @Override
  protected int next(final int bits) {
    recordEntropySpent(bits);
    return nextInt() & ((1 << bits) - 1);
  }

  @SuppressWarnings("NumericCastThatLosesPrecision")
  @Override
  public void nextBytes(final byte[] bytes) {
    final SplittableRandom local = getSplittableRandom();
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) (local.nextInt(256));
    }
    recordEntropySpent(bytes.length * 8L);
  }

  @Override
  public int nextInt() {
    final int out = getSplittableRandom().nextInt();
    recordEntropySpent(32);
    return out;
  }

  @Override
  public int nextInt(final int bound) {
    final int out = getSplittableRandom().nextInt(bound);
    recordEntropySpent(entropyOfInt(0, bound));
    return out;
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
  @EntryPoint
  public int nextInt(final int origin, final int bound) {
    final int out = getSplittableRandom().nextInt(origin, bound);
    recordEntropySpent(entropyOfInt(origin, bound));
    return out;
  }

  @Override
  public long nextLong() {
    final long out = getSplittableRandom().nextLong();
    recordEntropySpent(64);
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
  @EntryPoint
  public long nextLong(final long bound) {
    final long out = getSplittableRandom().nextLong(bound);
    recordEntropySpent(entropyOfLong(0, bound));
    return out;
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
  @EntryPoint
  public long nextLong(final long origin, final long bound) {
    final long out = getSplittableRandom().nextLong(origin, bound);
    recordEntropySpent(entropyOfLong(origin, bound));
    return out;
  }

  @Override
  public double nextDouble() {
    final double out = getSplittableRandom().nextDouble();
    recordEntropySpent(ENTROPY_OF_DOUBLE);
    return out;
  }

  @Override
  public synchronized double nextGaussian() {
    double output;
    if (haveNextNextGaussian) {
      haveNextNextGaussian = false;
      output = nextNextGaussian;
    } else {
      double v1, v2, s;
      do {
        v1 = 2 * getSplittableRandom().nextDouble() - 1; // between -1 and 1
        v2 = 2 * getSplittableRandom().nextDouble() - 1; // between -1 and 1
        s = v1 * v1 + v2 * v2;
      } while (s >= 1 || s == 0);
      double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
      nextNextGaussian = v2 * multiplier;
      haveNextNextGaussian = true;
      output = v1 * multiplier;
    }

    // Upper bound. 2 Gaussians are generated from *at least* 2, but sometimes more, nextDouble()
    // calls. But the entropy of the final output can't exceed 64 bits, since that's its length,
    // and must actually be smaller since the 2^64 possible values of a double aren't all equally
    // likely. But I don't know how *much* smaller.
    recordEntropySpent(64);

    return output;
  }

  /**
   * Returns a pseudorandom {@code double} value between 0.0 (inclusive) and the specified bound
   * (exclusive).
   *
   * @param bound the upper bound (exclusive).  Must be positive.
   * @return a pseudorandom {@code double} value between zero (inclusive) and the bound (exclusive)
   * @throws IllegalArgumentException if {@code bound} is not positive
   */
  @EntryPoint
  public double nextDouble(final double bound) {
    final double out = getSplittableRandom().nextDouble(bound);
    recordEntropySpent(ENTROPY_OF_DOUBLE);
    return out;
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
  @EntryPoint
  public double nextDouble(final double origin, final double bound) {
    final double out = getSplittableRandom().nextDouble(origin, bound);
    recordEntropySpent(ENTROPY_OF_DOUBLE);
    return out;
  }

  @Override
  public boolean nextBoolean() {
    final boolean out = getSplittableRandom().nextBoolean();
    recordEntropySpent(1);
    return out;
  }

  @Override
  public float nextFloat() {
    final float out = getSplittableRandom().nextInt(1 << ENTROPY_OF_FLOAT) /
        ((float) (1 << ENTROPY_OF_FLOAT));
    recordEntropySpent(ENTROPY_OF_FLOAT);
    return out;
  }

  @Override
  public IntStream ints(final long streamSize) {
    final IntStream out = getSplittableRandom().split().ints(streamSize);
    recordEntropySpent(streamSize * 32);
    return out;
  }

  @Override
  public IntStream ints() {
    final IntStream out = getSplittableRandom().split().ints();
    recordAllEntropySpent();
    return out;
  }

  @Override
  public IntStream ints(final long streamSize, final int randomNumberOrigin,
      final int randomNumberBound) {
    final IntStream out = getSplittableRandom().split()
        .ints(streamSize, randomNumberOrigin, randomNumberBound);
    recordEntropySpent(streamSize * entropyOfInt(randomNumberOrigin, randomNumberBound));
    return out;
  }

  @Override
  public IntStream ints(final int randomNumberOrigin, final int randomNumberBound) {
    final IntStream out = getSplittableRandom().split().ints(randomNumberOrigin, randomNumberBound);
    recordAllEntropySpent();
    return out;
  }

  @Override
  public LongStream longs(final long streamSize) {
    final LongStream out = getSplittableRandom().split().longs(streamSize);
    recordEntropySpent(64 * streamSize);
    return out;
  }

  @Override
  public LongStream longs() {
    final LongStream out = getSplittableRandom().split().longs();
    recordAllEntropySpent();
    return out;
  }

  @Override
  public LongStream longs(final long streamSize, final long randomNumberOrigin,
      final long randomNumberBound) {
    final LongStream out = getSplittableRandom().split()
        .longs(streamSize, randomNumberOrigin, randomNumberBound);
    recordEntropySpent(streamSize * entropyOfLong(randomNumberOrigin, randomNumberBound));
    return out;
  }

  @Override
  public LongStream longs(final long randomNumberOrigin, final long randomNumberBound) {
    final LongStream out = getSplittableRandom().split()
        .longs(randomNumberOrigin, randomNumberBound);
    recordAllEntropySpent();
    return out;
  }

  @Override
  public DoubleStream doubles(final long streamSize) {
    final DoubleStream out = getSplittableRandom().split().doubles(streamSize);
    recordEntropySpent(ENTROPY_OF_DOUBLE * streamSize);
    return out;
  }

  @Override
  public DoubleStream doubles() {
    final DoubleStream out = getSplittableRandom().split().doubles();
    recordAllEntropySpent();
    return out;
  }

  @Override
  public DoubleStream doubles(final long streamSize, final double randomNumberOrigin,
      final double randomNumberBound) {
    final DoubleStream out = getSplittableRandom().split()
        .doubles(streamSize, randomNumberOrigin, randomNumberBound);
    recordEntropySpent(ENTROPY_OF_DOUBLE * streamSize);
    return out;
  }

  @Override
  public DoubleStream doubles(final double randomNumberOrigin, final double randomNumberBound) {
    final DoubleStream out = getSplittableRandom().split()
        .doubles(randomNumberOrigin, randomNumberBound);
    recordAllEntropySpent();
    return out;
  }

  @Override
  public int getNewSeedLength(@UnknownInitialization BaseSplittableRandomAdapter this) {
    return SEED_LENGTH_BYTES;
  }
}
