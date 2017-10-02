package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.prng.BaseRandom;
import java.util.Random;
import java.util.SplittableRandom;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

/**
 * Abstract class for implementations of {@link BaseRandom} that wrap a {@link SplittableRandom}.
 *
 * @author Chris Hennick
 */
public abstract class BaseSplittableRandomAdapter extends BaseRandom {

  private static final long serialVersionUID = 4273652147052638879L;

  /**
   * Constructs an instance with the given seed.
   *
   * @param seed The seed.
   */
  public BaseSplittableRandomAdapter(final byte[] seed) {
    super(seed);
  }

  /**
   * Constructs an instance with the given seed.
   *
   * @param seed The seed.
   */
  public BaseSplittableRandomAdapter(final long seed) {
    super(seed);
  }

  @Override
  protected void setSeedInternal(
      @UnknownInitialization(Random.class)BaseSplittableRandomAdapter this, final byte[] seed) {
    super.setSeedInternal(seed);
  }

  /**
   * Returns the {@link SplittableRandom} that is to be used to generate random numbers for the
   * current calling context. Note that {@link SplittableRandom} instances aren't thread-safe.
   * Called by all the {@code next*} methods.
   *
   * @return a {@link SplittableRandom}.
   */
  protected abstract SplittableRandom getSplittableRandom();

  @Override
  public double nextDouble(double origin, double bound) {
    final double out = getSplittableRandom().nextDouble(origin, bound);
    recordEntropySpent(ENTROPY_OF_DOUBLE);
    return out;
  }

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
  protected boolean withProbabilityInternal(final double probability) {
    final boolean result = getSplittableRandom().nextDouble() < probability;
    // We're only outputting one bit
    recordEntropySpent(1);
    return result;
  }

  @Override
  public boolean preferSeedWithLong() {
    return true;
  }

  @Override
  protected long nextLongNoEntropyDebit() {
    return getSplittableRandom().nextLong();
  }

  @Override
  public long nextLong(final long bound) {
    final long out = getSplittableRandom().nextLong(bound);
    recordEntropySpent(entropyOfLong(0, bound));
    return out;
  }

  @Override
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
  public double nextGaussian() {
    // Upper bound. 2 Gaussians are generated from 2 nextDouble calls, which once made are either
    // used or rerolled.
    recordEntropySpent(ENTROPY_OF_DOUBLE);

    return internalNextGaussian(() -> getSplittableRandom().nextDouble());
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
    return Long.BYTES;
  }
}
