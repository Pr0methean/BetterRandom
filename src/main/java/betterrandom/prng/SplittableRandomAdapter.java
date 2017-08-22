package betterrandom.prng;

import betterrandom.seed.DefaultSeedGenerator;
import betterrandom.util.BinaryUtils;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class SplittableRandomAdapter extends Random {

  /** Singleton unless subclass used. */
  protected SplittableRandomAdapter() {}

  private static final SplittableRandomAdapter INSTANCE = new SplittableRandomAdapter();

  public static SplittableRandomAdapter getInstance() {
    return INSTANCE;
  }

  protected static final SplittableRandom UNDERLYING = new SplittableRandom(
      BinaryUtils.convertBytesToLong(DefaultSeedGenerator.getInstance().generateSeed(8), 0));
  private static final ThreadLocal<SplittableRandom> THREAD_LOCAL = ThreadLocal
      .withInitial(UNDERLYING::split);

  private SplittableRandom getLocal() {
    return THREAD_LOCAL.get();
  }

  @Override
  public synchronized void setSeed(long seed) {
    THREAD_LOCAL.set(new SplittableRandom(seed));
  }

  @Override
  protected int next(int bits) {
    return nextInt() & ((1 << bits) - 1);
  }

  @Override
  public void nextBytes(byte[] bytes) {
    SplittableRandom local = getLocal();
    for (int i=0; i<bytes.length; i++) {
      bytes[i] = (byte) (local.nextInt(256));
    }
  }

  @Override
  public int nextInt() {
    return getLocal().nextInt();
  }

  @Override
  public int nextInt(int bound) {
    return getLocal().nextInt(bound);
  }

  public int nextInt(int origin, int bound) {
    return getLocal().nextInt(origin, bound);
  }

  @Override
  public long nextLong() {
    return getLocal().nextLong();
  }

  public long nextLong(long bound) {
    return getLocal().nextLong(bound);
  }

  public long nextLong(long origin, long bound) {
    return getLocal().nextLong(origin, bound);
  }

  @Override
  public double nextDouble() {
    return getLocal().nextDouble();
  }

  public double nextDouble(double bound) {
    return getLocal().nextDouble(bound);
  }

  public double nextDouble(double origin, double bound) {
    return getLocal().nextDouble(origin, bound);
  }

  @Override
  public boolean nextBoolean() {
    return getLocal().nextBoolean();
  }

  @Override
  public IntStream ints(long streamSize) {
    return getLocal().ints(streamSize);
  }

  @Override
  public IntStream ints() {
    return getLocal().ints();
  }

  @Override
  public IntStream ints(long streamSize, int randomNumberOrigin,
      int randomNumberBound) {
    return getLocal().ints(streamSize, randomNumberOrigin, randomNumberBound);
  }

  @Override
  public IntStream ints(int randomNumberOrigin, int randomNumberBound) {
    return getLocal().ints(randomNumberOrigin, randomNumberBound);
  }

  @Override
  public LongStream longs(long streamSize) {
    return getLocal().longs(streamSize);
  }

  @Override
  public LongStream longs() {
    return getLocal().longs();
  }

  @Override
  public LongStream longs(long streamSize, long randomNumberOrigin,
      long randomNumberBound) {
    return getLocal().longs(streamSize, randomNumberOrigin, randomNumberBound);
  }

  @Override
  public LongStream longs(long randomNumberOrigin, long randomNumberBound) {
    return getLocal().longs(randomNumberOrigin, randomNumberBound);
  }

  @Override
  public DoubleStream doubles(long streamSize) {
    return getLocal().doubles(streamSize);
  }

  @Override
  public DoubleStream doubles() {
    return getLocal().doubles();
  }

  @Override
  public DoubleStream doubles(long streamSize, double randomNumberOrigin,
      double randomNumberBound) {
    return getLocal().doubles(streamSize, randomNumberOrigin, randomNumberBound);
  }

  @Override
  public DoubleStream doubles(double randomNumberOrigin, double randomNumberBound) {
    return getLocal().doubles(randomNumberOrigin, randomNumberBound);
  }
}
