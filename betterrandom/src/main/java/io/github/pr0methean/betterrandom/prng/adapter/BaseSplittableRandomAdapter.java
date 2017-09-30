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
    recordEntropySpent(bytes.length * (long) (Byte.SIZE));
  }

  @Override
  public int nextInt() {
    final int out = getSplittableRandom().nextInt();
    recordEntropySpent(Integer.SIZE);
    return out;
  }

  @Override
  public int nextInt(final int bound) {
    final int out = getSplittableRandom().nextInt(bound);
    recordEntropySpent(entropyOfInt(0, bound));
    return out;
  }

  @Override
  public int nextInt(final int origin, final int bound) {
    final int out = getSplittableRandom().nextInt(origin, bound);
    recordEntropySpent(entropyOfInt(origin, bound));
    return out;
  }

  @Override
  protected boolean withProbabilityInternal(double probability) {
    final boolean result = getSplittableRandom().nextDouble() < probability;
    // We're only outputting one bit
    recordEntropySpent(1);
    return result;
  }

  @Override
  protected long nextLongNoEntropyDebit() {
    return getSplittableRandom().nextLong();
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

  @Override
  public long nextLong(final long origin, final long bound) {
    long out = getSplittableRandom().nextLong(origin, bound);
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
  public double nextGaussian() {
    // Upper bound. 2 Gaussians are generated from 2 nextDouble calls, which once made are either
    // used or rerolled.
    recordEntropySpent(ENTROPY_OF_DOUBLE);

    return internalNextGaussian(() -> getSplittableRandom().nextDouble());
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
  public int getNewSeedLength(@UnknownInitialization BaseSplittableRandomAdapter this) {
    return SEED_LENGTH_BYTES;
  }
}
