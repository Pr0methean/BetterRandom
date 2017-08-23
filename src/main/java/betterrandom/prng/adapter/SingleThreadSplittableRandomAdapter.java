package betterrandom.prng.adapter;

import betterrandom.prng.BaseRandom;
import betterrandom.seed.SeedException;
import betterrandom.seed.SeedGenerator;
import betterrandom.util.BinaryUtils;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.SplittableRandom;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

public class SingleThreadSplittableRandomAdapter extends BaseRandom {

  public static final int SEED_LENGTH_BYTES = 8;
  private static final long serialVersionUID = -1125374167384636394L;
  @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
  protected transient SplittableRandom underlying; // a SplittableRandom is not Serializable
  protected boolean deserializedAndNotUsedSince = false;

  public SingleThreadSplittableRandomAdapter(SeedGenerator seedGenerator) throws SeedException {
    super(seedGenerator.generateSeed(SEED_LENGTH_BYTES));
    underlying = new SplittableRandom(
        BinaryUtils.convertBytesToLong(seed, 0));
  }

  // Overridden in the subclass
  protected SplittableRandom getSplittableRandom() {
    return underlying;
  }

  @EnsuresNonNull("underlying")
  @Override
  protected void initTransientFields(
      @UnknownInitialization SingleThreadSplittableRandomAdapter this) {
    super.initTransientFields();
    setSeed(seed);
  }

  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    ois.defaultReadObject();
    initTransientFields();
    if (!deserializedAndNotUsedSince) {
      underlying = underlying.split(); // Ensures we aren't rewinding
    }
    deserializedAndNotUsedSince = true; // Ensures serializing and deserializing is idempotent
  }

  @Override
  public synchronized void setSeed(long seed) {
    if (!superConstructorFinished) {
      return; // Cannot work when called from Random.<init>
    }
    underlying = new SplittableRandom(seed);
    super.setSeed(BinaryUtils.convertLongToBytes(seed));
  }

  @Override
  public synchronized void setSeed(byte[] seed) {
    underlying = new SplittableRandom(BinaryUtils.convertBytesToLong(seed, 0));
    super.setSeed(seed);
  }

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

  @Override
  public boolean equals(Object o) {
    return this == o
        || (o instanceof SingleThreadSplittableRandomAdapter
        && underlying.equals(((SingleThreadSplittableRandomAdapter) o).underlying));
  }

  @Override
  public int hashCode() {
    return underlying.hashCode() + 1;
  }
}
