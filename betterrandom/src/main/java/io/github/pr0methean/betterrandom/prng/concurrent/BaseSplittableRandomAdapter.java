package io.github.pr0methean.betterrandom.prng.concurrent;

import io.github.pr0methean.betterrandom.prng.BaseRandom;
import java.util.SplittableRandom;

/**
 * Abstract class for implementations of {@link BaseRandom} that wrap one or more {@link
 * SplittableRandom} instances.
 *
 * @author Chris Hennick
 */
public abstract class BaseSplittableRandomAdapter extends BaseRandom {

  private static final long serialVersionUID = 4273652147052638879L;
  private static final float FLOAT_MULTIPLIER = 1.0f / (1 << ENTROPY_OF_FLOAT);

  /**
   * Constructs an instance with the given seed.
   *
   * @param seed The seed.
   */
  protected BaseSplittableRandomAdapter(final byte[] seed) {
    super(seed);
  }

  /**
   * Constructs an instance with the given seed.
   *
   * @param seed The seed.
   */
  protected BaseSplittableRandomAdapter(final long seed) {
    super(seed);
  }

  /**
   * Returns the {@link SplittableRandom} that is to be used to generate random numbers for the
   * current thread. ({@link SplittableRandom} isn't thread-safe.) Called by all the {@code next*}
   * methods.
   *
   * @return the {@link SplittableRandom} to use with the current thread.
   */
  protected abstract SplittableRandom getSplittableRandom();

  /**
   * Delegates to {@link SplittableRandom#nextDouble(double) SplittableRandom.nextDouble(bound)}.
   */
  @Override public double nextDouble(final double bound) {
    debitEntropy(ENTROPY_OF_DOUBLE);
    return getSplittableRandom().nextDouble(bound);
  }

  /**
   * Delegates to {@link SplittableRandom#nextDouble(double, double) SplittableRandom.nextDouble
   * (origin,
   * bound)}.
   */
  @Override public double nextDouble(final double origin, final double bound) {
    debitEntropy(ENTROPY_OF_DOUBLE);
    return getSplittableRandom().nextDouble(origin, bound);
  }

  /**
   * Delegates to {@link SplittableRandom#nextInt()} or {@link SplittableRandom#nextInt(int)}.
   */
  @Override protected int next(final int bits) {
    debitEntropy(bits);
    return (bits >= 32) ? getSplittableRandom().nextInt() :
        (bits == 31) ? getSplittableRandom().nextInt() >>> 1 :
            getSplittableRandom().nextInt(1 << bits);
  }

  /**
   * Delegates to {@link SplittableRandom#nextInt(int) SplittableRandom.nextInt(256)}.
   */
  @SuppressWarnings("NumericCastThatLosesPrecision") @Override public void nextBytes(
      final byte[] bytes) {
    for (int i = 0; i < bytes.length; i++) {
      debitEntropy(Byte.SIZE); // May cause replacement before the next getSplittableRandom()
      bytes[i] = (byte) (getSplittableRandom().nextInt(1 << Byte.SIZE));
    }
  }

  /**
   * Delegates to {@link SplittableRandom#nextInt()}.
   */
  @Override public int nextInt() {
    debitEntropy(Integer.SIZE);
    return getSplittableRandom().nextInt();
  }

  /**
   * Delegates to {@link SplittableRandom#nextInt(int) SplittableRandom.nextInt(bound)}.
   */
  @Override public int nextInt(final int bound) {
    debitEntropy(entropyOfInt(0, bound));
    return getSplittableRandom().nextInt(bound);
  }

  /**
   * Delegates to {@link SplittableRandom#nextInt(int, int) SplittableRandom.nextInt(origin,
   * bound)}.
   */
  @Override public int nextInt(final int origin, final int bound) {
    debitEntropy(entropyOfInt(origin, bound));
    return getSplittableRandom().nextInt(origin, bound);
  }

  /**
   * <p>Delegates to {@link SplittableRandom#nextDouble()}.</p> {@inheritDoc}
   */
  @Override protected boolean withProbabilityInternal(final double probability) {
    // We're only outputting one bit
    debitEntropy(1);
    return getSplittableRandom().nextDouble() < probability;
  }

  @Override public boolean preferSeedWithLong() {
    return true;
  }

  /**
   * <p>Delegates to {@link SplittableRandom#nextLong()}.</p> {@inheritDoc}
   */
  @Override protected long nextLongNoEntropyDebit() {
    return getSplittableRandom().nextLong();
  }

  /**
   * Delegates to {@link SplittableRandom#nextLong(long) SplittableRandom.nextLong(bound)}.
   */
  @Override public long nextLong(final long bound) {
    debitEntropy(entropyOfLong(0, bound));
    return getSplittableRandom().nextLong(bound);
  }

  /**
   * Delegates to {@link SplittableRandom#nextLong(long, long) SplittableRandom.nextLong(origin,
   * bound)}.
   */
  @Override public long nextLong(final long origin, final long bound) {
    debitEntropy(entropyOfLong(origin, bound));
    return getSplittableRandom().nextLong(origin, bound);
  }

  /**
   * Delegates to {@link SplittableRandom#nextDouble()}.
   */
  @Override protected double nextDoubleNoEntropyDebit() {
    return getSplittableRandom().nextDouble();
  }

  /**
   * Delegates to {@link SplittableRandom#nextDouble()} via
   * {@link #internalNextGaussian(java.util.function.DoubleSupplier)}.
   */
  @Override public double nextGaussian() {
    // Upper bound. 2 Gaussians are generated from 2 nextDouble calls, which once made are either
    // used or rerolled.
    debitEntropy(ENTROPY_OF_DOUBLE);
    return internalNextGaussian(() -> getSplittableRandom().nextDouble());
  }

  @Override protected void lockForNextGaussian() {
    // No-op.
  }

  @Override protected void unlockForNextGaussian() {
    // No-op.
  }

  /**
   * Delegates to {@link SplittableRandom#nextBoolean()}.
   */
  @Override public boolean nextBoolean() {
    debitEntropy(1);
    return getSplittableRandom().nextBoolean();
  }

  /**
   * Delegates to {@link SplittableRandom#nextInt(int)}.
   */
  @Override public float nextFloat() {
    debitEntropy(ENTROPY_OF_FLOAT);
    return getSplittableRandom().nextInt(1 << ENTROPY_OF_FLOAT)
        * FLOAT_MULTIPLIER;
  }

  /**
   * Returns the only supported seed length.
   */
  @Override public int getNewSeedLength() {
    return Long.BYTES;
  }
}
