package io.github.pr0methean.betterrandom.prng.adapter;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.prng.BaseEntropyCountingRandom;
import io.github.pr0methean.betterrandom.util.EntryPoint;
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
public abstract class BaseSplittableRandomAdapter extends BaseEntropyCountingRandom {

  /** Constant {@code SEED_LENGTH_BYTES=8} */
  public static final int SEED_LENGTH_BYTES = 8;
  private static final long serialVersionUID = 4273652147052638879L;

  /**
   * <p>Constructor for BaseSplittableRandomAdapter.</p>
   *
   * @param seed an array of byte.
   */
  public BaseSplittableRandomAdapter(final byte[] seed) {
    super(seed);
  }

  @Override
  protected ToStringHelper addSubSubclassFields(final ToStringHelper original) {
    return original;
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

  @Override
  public void nextBytes(final byte[] bytes) {
    final SplittableRandom local = getSplittableRandom();
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) (local.nextInt(256));
    }
    recordEntropySpent(bytes.length * 8);
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
