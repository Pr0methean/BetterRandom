package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.prng.BaseRandom;
import java.util.SplittableRandom;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public abstract class BaseSplittableRandomAdapter extends BaseRandom {

  public static final int SEED_LENGTH_BYTES = 8;
  private static final long serialVersionUID = 4273652147052638879L;

  public BaseSplittableRandomAdapter(final byte[] seed) {
    super(seed);
  }

  protected abstract SplittableRandom getSplittableRandom();

  @Override
  protected int next(final int bits) {

    return nextInt() & ((1 << bits) - 1);
  }

  @Override
  public void nextBytes(final byte[] bytes) {

    final SplittableRandom local = getSplittableRandom();
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) (local.nextInt(256));
    }
  }

  @Override
  public int nextInt() {

    return getSplittableRandom().nextInt();
  }

  @Override
  public int nextInt(final int bound) {

    return getSplittableRandom().nextInt(bound);
  }

  public int nextInt(final int origin, final int bound) {

    return getSplittableRandom().nextInt(origin, bound);
  }

  @Override
  public long nextLong() {

    return getSplittableRandom().nextLong();
  }

  public long nextLong(final long bound) {

    return getSplittableRandom().nextLong(bound);
  }

  public long nextLong(final long origin, final long bound) {

    return getSplittableRandom().nextLong(origin, bound);
  }

  @Override
  public double nextDouble() {

    return getSplittableRandom().nextDouble();
  }

  public double nextDouble(final double bound) {

    return getSplittableRandom().nextDouble(bound);
  }

  public double nextDouble(final double origin, final double bound) {

    return getSplittableRandom().nextDouble(origin, bound);
  }

  @Override
  public boolean nextBoolean() {

    return getSplittableRandom().nextBoolean();
  }

  @Override
  public IntStream ints(final long streamSize) {

    return getSplittableRandom().ints(streamSize);
  }

  @Override
  public IntStream ints() {

    return getSplittableRandom().ints();
  }

  @Override
  public IntStream ints(final long streamSize, final int randomNumberOrigin,
      final int randomNumberBound) {

    return getSplittableRandom().ints(streamSize, randomNumberOrigin, randomNumberBound);
  }

  @Override
  public IntStream ints(final int randomNumberOrigin, final int randomNumberBound) {

    return getSplittableRandom().ints(randomNumberOrigin, randomNumberBound);
  }

  @Override
  public LongStream longs(final long streamSize) {

    return getSplittableRandom().longs(streamSize);
  }

  @Override
  public LongStream longs() {

    return getSplittableRandom().longs();
  }

  @Override
  public LongStream longs(final long streamSize, final long randomNumberOrigin,
      final long randomNumberBound) {

    return getSplittableRandom().longs(streamSize, randomNumberOrigin, randomNumberBound);
  }

  @Override
  public LongStream longs(final long randomNumberOrigin, final long randomNumberBound) {

    return getSplittableRandom().longs(randomNumberOrigin, randomNumberBound);
  }

  @Override
  public DoubleStream doubles(final long streamSize) {

    return getSplittableRandom().doubles(streamSize);
  }

  @Override
  public DoubleStream doubles() {

    return getSplittableRandom().doubles();
  }

  @Override
  public DoubleStream doubles(final long streamSize, final double randomNumberOrigin,
      final double randomNumberBound) {

    return getSplittableRandom().doubles(streamSize, randomNumberOrigin, randomNumberBound);
  }

  @Override
  public DoubleStream doubles(final double randomNumberOrigin, final double randomNumberBound) {

    return getSplittableRandom().doubles(randomNumberOrigin, randomNumberBound);
  }

  @Override
  public int getNewSeedLength() {
    return SEED_LENGTH_BYTES;
  }
}
