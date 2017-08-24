package betterrandom.prng.adapter;

import betterrandom.prng.BaseRandom;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.SplittableRandom;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

public abstract class BaseSplittableRandomAdapter extends BaseRandom {

  public static final int SEED_LENGTH_BYTES = 8;
  private static final long serialVersionUID = 4273652147052638879L;

  public BaseSplittableRandomAdapter(byte[] seed) {
    super(seed);
  }

  protected abstract SplittableRandom getSplittableRandom();

  @Override
  protected int next(int bits) {

    return nextInt() & ((1 << bits) - 1);
  }

  @Override
  public void nextBytes(byte[] bytes) {

    SplittableRandom local = getSplittableRandom();
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) (local.nextInt(256));
    }
  }

  @Override
  public int nextInt() {

    return getSplittableRandom().nextInt();
  }

  @Override
  public int nextInt(int bound) {

    return getSplittableRandom().nextInt(bound);
  }

  public int nextInt(int origin, int bound) {

    return getSplittableRandom().nextInt(origin, bound);
  }

  @Override
  public long nextLong() {

    return getSplittableRandom().nextLong();
  }

  public long nextLong(long bound) {

    return getSplittableRandom().nextLong(bound);
  }

  public long nextLong(long origin, long bound) {

    return getSplittableRandom().nextLong(origin, bound);
  }

  @Override
  public double nextDouble() {

    return getSplittableRandom().nextDouble();
  }

  public double nextDouble(double bound) {

    return getSplittableRandom().nextDouble(bound);
  }

  public double nextDouble(double origin, double bound) {

    return getSplittableRandom().nextDouble(origin, bound);
  }

  @Override
  public boolean nextBoolean() {

    return getSplittableRandom().nextBoolean();
  }

  @Override
  public IntStream ints(long streamSize) {

    return getSplittableRandom().ints(streamSize);
  }

  @Override
  public IntStream ints() {

    return getSplittableRandom().ints();
  }

  @Override
  public IntStream ints(long streamSize, int randomNumberOrigin,
      int randomNumberBound) {

    return getSplittableRandom().ints(streamSize, randomNumberOrigin, randomNumberBound);
  }

  @Override
  public IntStream ints(int randomNumberOrigin, int randomNumberBound) {

    return getSplittableRandom().ints(randomNumberOrigin, randomNumberBound);
  }

  @Override
  public LongStream longs(long streamSize) {

    return getSplittableRandom().longs(streamSize);
  }

  @Override
  public LongStream longs() {

    return getSplittableRandom().longs();
  }

  @Override
  public LongStream longs(long streamSize, long randomNumberOrigin,
      long randomNumberBound) {

    return getSplittableRandom().longs(streamSize, randomNumberOrigin, randomNumberBound);
  }

  @Override
  public LongStream longs(long randomNumberOrigin, long randomNumberBound) {

    return getSplittableRandom().longs(randomNumberOrigin, randomNumberBound);
  }

  @Override
  public DoubleStream doubles(long streamSize) {

    return getSplittableRandom().doubles(streamSize);
  }

  @Override
  public DoubleStream doubles() {

    return getSplittableRandom().doubles();
  }

  @Override
  public DoubleStream doubles(long streamSize, double randomNumberOrigin,
      double randomNumberBound) {

    return getSplittableRandom().doubles(streamSize, randomNumberOrigin, randomNumberBound);
  }

  @Override
  public DoubleStream doubles(double randomNumberOrigin, double randomNumberBound) {

    return getSplittableRandom().doubles(randomNumberOrigin, randomNumberBound);
  }

  @Override
  public int getNewSeedLength() {
    return SEED_LENGTH_BYTES;
  }

  @RequiresNonNull({"seed", "lock"})
  protected void initSubclassTransientFields(@UnknownInitialization BaseRandom this) {
  }

  @Override
  @SuppressWarnings("OverriddenMethodCallDuringObjectConstruction")
  @EnsuresNonNull({"lock", "seed"})
  protected void readObject(ObjectInputStream in) throws IOException,
      ClassNotFoundException {
    in.defaultReadObject();
    initSubclassTransientFields();
  }
}
