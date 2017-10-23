package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.util.Java8Constants;
import java8.util.SplittableRandom;
import java8.util.function.DoubleSupplier;

/**
 * Abstract class for implementations of {@link BaseRandom} that wrap one or more {@link
 * SplittableRandom} instances.
 * @author Chris Hennick
 */
public abstract class BaseSplittableRandomAdapter extends BaseRandom {

  private static final long serialVersionUID = 4273652147052638879L;

  /**
   * Constructs an instance with the given seed.
   * @param seed The seed.
   */
  protected BaseSplittableRandomAdapter(final byte[] seed) {
    super(seed);
  }

  /**
   * Constructs an instance with the given seed.
   * @param seed The seed.
   */
  protected BaseSplittableRandomAdapter(final long seed) {
    super(seed);
  }

  @Override protected void setSeedInternal(final byte[] seed) {
    super.setSeedInternal(seed);
  }

  /**
   * Returns the {@link SplittableRandom} that is to be used to generate random numbers for the
   * current thread. ({@link SplittableRandom} isn't thread-safe.) Called by all the {@code next*}
   * methods.
   * @return the {@link SplittableRandom} to use with the current thread.
   */
  protected abstract SplittableRandom getSplittableRandom();

  /** Delegates to {@link SplittableRandom#nextDouble(double) SplittableRandom.nextDouble(bound)}. */
  @Override public double nextDouble(final double bound) {
    final double out = getSplittableRandom().nextDouble(bound);
    recordEntropySpent(ENTROPY_OF_DOUBLE);
    return out;
  }

  /**
   * Delegates to {@link SplittableRandom#nextDouble(double, double) SplittableRandom.nextDouble(origin,
   * bound)}.
   */
  @Override public double nextDouble(final double origin, final double bound) {
    final double out = getSplittableRandom().nextDouble(origin, bound);
    recordEntropySpent(ENTROPY_OF_DOUBLE);
    return out;
  }

  /** Delegates to {@link SplittableRandom#nextInt()} or {@link SplittableRandom#nextInt(int)}. */
  @Override protected int next(final int bits) {
    recordEntropySpent(bits);
    return (bits >= 32) ? getSplittableRandom().nextInt()
        : getSplittableRandom().nextInt(1 << (bits - 1));
  }

  /** Delegates to {@link SplittableRandom#nextInt(int) SplittableRandom.nextInt(256)}. */
  @SuppressWarnings("NumericCastThatLosesPrecision") @Override public void nextBytes(
      final byte[] bytes) {
    final SplittableRandom local = getSplittableRandom();
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) (local.nextInt(256));
    }
    recordEntropySpent(bytes.length * (long) (Byte.SIZE));
  }

  /** Delegates to {@link SplittableRandom#nextInt()}. */
  @Override public int nextInt() {
    final int out = getSplittableRandom().nextInt();
    recordEntropySpent(Integer.SIZE);
    return out;
  }

  /** Delegates to {@link SplittableRandom#nextInt(int) SplittableRandom.nextInt(bound)}. */
  @Override public int nextInt(final int bound) {
    final int out = getSplittableRandom().nextInt(bound);
    recordEntropySpent(entropyOfInt(0, bound));
    return out;
  }

  /**
   * Delegates to {@link SplittableRandom#nextInt(int, int) SplittableRandom.nextInt(origin,
   * bound)}.
   */
  @Override public int nextInt(final int origin, final int bound) {
    final int out = getSplittableRandom().nextInt(origin, bound);
    recordEntropySpent(entropyOfInt(origin, bound));
    return out;
  }

  /** <p>Delegates to {@link SplittableRandom#nextDouble()}.</p> {@inheritDoc} */
  @Override protected boolean withProbabilityInternal(final double probability) {
    final boolean result = getSplittableRandom().nextDouble() < probability;
    // We're only outputting one bit
    recordEntropySpent(1);
    return result;
  }

  @Override public boolean preferSeedWithLong() {
    return true;
  }

  /** <p>Delegates to {@link SplittableRandom#nextLong()}.</p> {@inheritDoc} */
  @Override protected long nextLongNoEntropyDebit() {
    return getSplittableRandom().nextLong();
  }

  /** Delegates to {@link SplittableRandom#nextLong(long) SplittableRandom.nextLong(bound)}. */
  @Override public long nextLong(final long bound) {
    final long out = getSplittableRandom().nextLong(bound);
    recordEntropySpent(entropyOfLong(0, bound));
    return out;
  }

  /**
   * Delegates to {@link SplittableRandom#nextLong(long, long) SplittableRandom.nextLong(origin,
   * bound)}.
   */
  @Override public long nextLong(final long origin, final long bound) {
    final long out = getSplittableRandom().nextLong(origin, bound);
    recordEntropySpent(entropyOfLong(origin, bound));
    return out;
  }

  /** Delegates to {@link SplittableRandom#nextDouble()}. */
  @Override public double nextDouble() {
    final double out = getSplittableRandom().nextDouble();
    recordEntropySpent(ENTROPY_OF_DOUBLE);
    return out;
  }

  /**
   * Delegates to {@link SplittableRandom#nextDouble()} via {@link #internalNextGaussian(DoubleSupplier)}.
   */
  @Override public double nextGaussian() {
    // Upper bound. 2 Gaussians are generated from 2 nextDouble calls, which once made are either
    // used or rerolled.
    recordEntropySpent(ENTROPY_OF_DOUBLE);

    return internalNextGaussian(new DoubleSupplier() {
      @Override public double getAsDouble() {
        return BaseSplittableRandomAdapter.this.getSplittableRandom().nextDouble();
      }
    });
  }

  /** Delegates to {@link SplittableRandom#nextBoolean()}. */
  @Override public boolean nextBoolean() {
    final boolean out = getSplittableRandom().nextBoolean();
    recordEntropySpent(1);
    return out;
  }

  /** Delegates to {@link SplittableRandom#nextInt(int)}. */
  @Override public float nextFloat() {
    final float out =
        getSplittableRandom().nextInt(1 << ENTROPY_OF_FLOAT) / ((float) (1 << ENTROPY_OF_FLOAT));
    recordEntropySpent(ENTROPY_OF_FLOAT);
    return out;
  }

  /** Returns the only supported seed length. */
  @Override public int getNewSeedLength() {
    return Java8Constants.LONG_BYTES;
  }
}
