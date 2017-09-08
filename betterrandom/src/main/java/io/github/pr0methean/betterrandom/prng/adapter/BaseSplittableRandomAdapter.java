package io.github.pr0methean.betterrandom.prng.adapter;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.prng.BaseEntropyCountingRandom;
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

  /** Constant <code>SEED_LENGTH_BYTES=8</code> */
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

  /** {@inheritDoc} */
  @Override
  public ToStringHelper addSubclassFields(final ToStringHelper original) {
    return original;
  }

  /**
   * <p>getSplittableRandom.</p>
   *
   * @return a {@link java.util.SplittableRandom} object.
   */
  protected abstract SplittableRandom getSplittableRandom();

  /** {@inheritDoc} */
  @Override
  protected int next(final int bits) {
    recordEntropySpent(bits);
    return nextInt() & ((1 << bits) - 1);
  }

  /** {@inheritDoc} */
  @Override
  public void nextBytes(final byte[] bytes) {
    final SplittableRandom local = getSplittableRandom();
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) (local.nextInt(256));
    }
    recordEntropySpent(bytes.length * 8);
  }

  /** {@inheritDoc} */
  @Override
  public int nextInt() {
    final int out = getSplittableRandom().nextInt();
    recordEntropySpent(32);
    return out;
  }

  /** {@inheritDoc} */
  @Override
  public int nextInt(final int bound) {
    final int out = getSplittableRandom().nextInt(bound);
    recordEntropySpent(entropyOfInt(0, bound));
    return out;
  }

  /**
   * <p>nextInt.</p>
   *
   * @param origin a int.
   * @param bound a int.
   * @return a int.
   */
  public int nextInt(final int origin, final int bound) {
    final int out = getSplittableRandom().nextInt(origin, bound);
    recordEntropySpent(entropyOfInt(origin, bound));
    return out;
  }

  /** {@inheritDoc} */
  @Override
  public long nextLong() {
    final long out = getSplittableRandom().nextLong();
    recordEntropySpent(64);
    return out;
  }

  /**
   * <p>nextLong.</p>
   *
   * @param bound a long.
   * @return a long.
   */
  public long nextLong(final long bound) {
    final long out = getSplittableRandom().nextLong(bound);
    recordEntropySpent(entropyOfLong(0, bound));
    return out;
  }

  /**
   * <p>nextLong.</p>
   *
   * @param origin a long.
   * @param bound a long.
   * @return a long.
   */
  public long nextLong(final long origin, final long bound) {
    final long out = getSplittableRandom().nextLong(origin, bound);
    recordEntropySpent(entropyOfLong(origin, bound));
    return out;
  }

  /** {@inheritDoc} */
  @Override
  public double nextDouble() {
    final double out = getSplittableRandom().nextDouble();
    recordEntropySpent(53);
    return out;
  }

  /**
   * <p>nextDouble.</p>
   *
   * @param bound a double.
   * @return a double.
   */
  public double nextDouble(final double bound) {
    final double out = getSplittableRandom().nextDouble(bound);
    recordEntropySpent(53);
    return out;
  }

  /**
   * <p>nextDouble.</p>
   *
   * @param origin a double.
   * @param bound a double.
   * @return a double.
   */
  public double nextDouble(final double origin, final double bound) {
    final double out = getSplittableRandom().nextDouble(origin, bound);
    recordEntropySpent(53);
    return out;
  }

  /** {@inheritDoc} */
  @Override
  public boolean nextBoolean() {
    final boolean out = getSplittableRandom().nextBoolean();
    recordEntropySpent(1);
    return out;
  }

  /** {@inheritDoc} */
  @Override
  public IntStream ints(final long streamSize) {
    final IntStream out = getSplittableRandom().split().ints(streamSize);
    recordEntropySpent(streamSize * 32);
    return out;
  }

  /** {@inheritDoc} */
  @Override
  public IntStream ints() {
    final IntStream out = getSplittableRandom().split().ints();
    recordAllEntropySpent();
    return out;
  }

  /** {@inheritDoc} */
  @Override
  public IntStream ints(final long streamSize, final int randomNumberOrigin,
      final int randomNumberBound) {
    final IntStream out = getSplittableRandom().split()
        .ints(streamSize, randomNumberOrigin, randomNumberBound);
    recordEntropySpent(streamSize * entropyOfInt(randomNumberOrigin, randomNumberBound));
    return out;
  }

  /** {@inheritDoc} */
  @Override
  public IntStream ints(final int randomNumberOrigin, final int randomNumberBound) {
    final IntStream out = getSplittableRandom().split().ints(randomNumberOrigin, randomNumberBound);
    recordAllEntropySpent();
    return out;
  }

  private void recordAllEntropySpent() {
    entropyBits.set(0);
    if (seederThread != null) {
      seederThread.asyncReseed(this);
    }
  }

  /** {@inheritDoc} */
  @Override
  public LongStream longs(final long streamSize) {
    final LongStream out = getSplittableRandom().split().longs(streamSize);
    recordEntropySpent(64 * streamSize);
    return out;
  }

  /** {@inheritDoc} */
  @Override
  public LongStream longs() {
    final LongStream out = getSplittableRandom().split().longs();
    recordAllEntropySpent();
    return out;
  }

  /** {@inheritDoc} */
  @Override
  public LongStream longs(final long streamSize, final long randomNumberOrigin,
      final long randomNumberBound) {
    final LongStream out = getSplittableRandom().split()
        .longs(streamSize, randomNumberOrigin, randomNumberBound);
    recordEntropySpent(streamSize * entropyOfLong(randomNumberOrigin, randomNumberBound));
    return out;
  }

  /** {@inheritDoc} */
  @Override
  public LongStream longs(final long randomNumberOrigin, final long randomNumberBound) {
    final LongStream out = getSplittableRandom().split()
        .longs(randomNumberOrigin, randomNumberBound);
    recordAllEntropySpent();
    return out;
  }

  /** {@inheritDoc} */
  @Override
  public DoubleStream doubles(final long streamSize) {
    final DoubleStream out = getSplittableRandom().split().doubles(streamSize);
    recordEntropySpent(53 * streamSize);
    return out;
  }

  /** {@inheritDoc} */
  @Override
  public DoubleStream doubles() {
    final DoubleStream out = getSplittableRandom().split().doubles();
    recordAllEntropySpent();
    return out;
  }

  /** {@inheritDoc} */
  @Override
  public DoubleStream doubles(final long streamSize, final double randomNumberOrigin,
      final double randomNumberBound) {
    final DoubleStream out = getSplittableRandom().split()
        .doubles(streamSize, randomNumberOrigin, randomNumberBound);
    recordEntropySpent(53 * streamSize);
    return out;
  }

  /** {@inheritDoc} */
  @Override
  public DoubleStream doubles(final double randomNumberOrigin, final double randomNumberBound) {
    final DoubleStream out = getSplittableRandom().split()
        .doubles(randomNumberOrigin, randomNumberBound);
    recordAllEntropySpent();
    return out;
  }

  /** {@inheritDoc} */
  @Override
  public int getNewSeedLength(@UnknownInitialization BaseSplittableRandomAdapter this) {
    return SEED_LENGTH_BYTES;
  }
}
