package betterrandom.prng.adapter;

import betterrandom.prng.BaseRandom;
import java.util.SplittableRandom;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public abstract class BaseSplittableRandomAdapter extends BaseRandom {

  public static final int SEED_LENGTH_BYTES = 8;
  @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
  protected boolean deserializedAndNotUsedSince = false;

  public BaseSplittableRandomAdapter(byte[] seed) {
    super(seed);
  }

  protected abstract SplittableRandom getSplittableRandom();

  @Override
  protected int next(int bits) {
    deserializedAndNotUsedSince = false;
    return nextInt() & ((1 << bits) - 1);
  }

  @Override
  public void nextBytes(byte[] bytes) {
    deserializedAndNotUsedSince = false;
    SplittableRandom local = getSplittableRandom();
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) (local.nextInt(256));
    }
  }

  @Override
  public int nextInt() {
    deserializedAndNotUsedSince = false;
    return getSplittableRandom().nextInt();
  }

  @Override
  public int nextInt(int bound) {
    deserializedAndNotUsedSince = false;
    return getSplittableRandom().nextInt(bound);
  }

  public int nextInt(int origin, int bound) {
    deserializedAndNotUsedSince = false;
    return getSplittableRandom().nextInt(origin, bound);
  }

  @Override
  public long nextLong() {
    deserializedAndNotUsedSince = false;
    return getSplittableRandom().nextLong();
  }

  public long nextLong(long bound) {
    deserializedAndNotUsedSince = false;
    return getSplittableRandom().nextLong(bound);
  }

  public long nextLong(long origin, long bound) {
    deserializedAndNotUsedSince = false;
    return getSplittableRandom().nextLong(origin, bound);
  }

  @Override
  public double nextDouble() {
    deserializedAndNotUsedSince = false;
    return getSplittableRandom().nextDouble();
  }

  public double nextDouble(double bound) {
    deserializedAndNotUsedSince = false;
    return getSplittableRandom().nextDouble(bound);
  }

  public double nextDouble(double origin, double bound) {
    deserializedAndNotUsedSince = false;
    return getSplittableRandom().nextDouble(origin, bound);
  }

  @Override
  public boolean nextBoolean() {
    deserializedAndNotUsedSince = false;
    return getSplittableRandom().nextBoolean();
  }

  @Override
  public IntStream ints(long streamSize) {
    deserializedAndNotUsedSince = false;
    return getSplittableRandom().ints(streamSize);
  }

  @Override
  public IntStream ints() {
    deserializedAndNotUsedSince = false;
    return getSplittableRandom().ints();
  }

  @Override
  public IntStream ints(long streamSize, int randomNumberOrigin,
      int randomNumberBound) {
    deserializedAndNotUsedSince = false;
    return getSplittableRandom().ints(streamSize, randomNumberOrigin, randomNumberBound);
  }

  @Override
  public IntStream ints(int randomNumberOrigin, int randomNumberBound) {
    deserializedAndNotUsedSince = false;
    return getSplittableRandom().ints(randomNumberOrigin, randomNumberBound);
  }

  @Override
  public LongStream longs(long streamSize) {
    deserializedAndNotUsedSince = false;
    return getSplittableRandom().longs(streamSize);
  }

  @Override
  public LongStream longs() {
    deserializedAndNotUsedSince = false;
    return getSplittableRandom().longs();
  }

  @Override
  public LongStream longs(long streamSize, long randomNumberOrigin,
      long randomNumberBound) {
    deserializedAndNotUsedSince = false;
    return getSplittableRandom().longs(streamSize, randomNumberOrigin, randomNumberBound);
  }

  @Override
  public LongStream longs(long randomNumberOrigin, long randomNumberBound) {
    deserializedAndNotUsedSince = false;
    return getSplittableRandom().longs(randomNumberOrigin, randomNumberBound);
  }

  @Override
  public DoubleStream doubles(long streamSize) {
    deserializedAndNotUsedSince = false;
    return getSplittableRandom().doubles(streamSize);
  }

  @Override
  public DoubleStream doubles() {
    deserializedAndNotUsedSince = false;
    return getSplittableRandom().doubles();
  }

  @Override
  public DoubleStream doubles(long streamSize, double randomNumberOrigin,
      double randomNumberBound) {
    deserializedAndNotUsedSince = false;
    return getSplittableRandom().doubles(streamSize, randomNumberOrigin, randomNumberBound);
  }

  @Override
  public DoubleStream doubles(double randomNumberOrigin, double randomNumberBound) {
    deserializedAndNotUsedSince = false;
    return getSplittableRandom().doubles(randomNumberOrigin, randomNumberBound);
  }

  @Override
  public int getNewSeedLength() {
    return SEED_LENGTH_BYTES;
  }
}
