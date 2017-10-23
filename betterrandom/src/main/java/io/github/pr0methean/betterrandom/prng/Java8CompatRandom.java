package io.github.pr0methean.betterrandom.prng;

import java.io.Serializable;
import java8.util.stream.DoubleStream;
import java8.util.stream.IntStream;
import java8.util.stream.LongStream;

public interface Java8CompatRandom extends Serializable {

  @SuppressWarnings("NumericCastThatLosesPrecision") void nextBytes(byte[] bytes);

  int nextInt();

  int nextInt(int bound);

  long nextLong();

  DoubleStream doubles(double randomNumberOrigin, double randomNumberBound);

  DoubleStream doubles();

  DoubleStream doubles(long streamSize);

  DoubleStream doubles(long streamSize, double randomNumberOrigin, double randomNumberBound);

  boolean nextBoolean();

  float nextFloat();

  double nextDouble();

  double nextGaussian();

  IntStream ints(long streamSize);

  IntStream ints();

  IntStream ints(long streamSize, int randomNumberOrigin, int randomNumberBound);

  IntStream ints(int randomNumberOrigin, int randomNumberBound);

  LongStream longs(long streamSize);

  LongStream longs();

  LongStream longs(long streamSize, long randomNumberOrigin, long randomNumberBound);

  LongStream longs(long randomNumberOrigin, long randomNumberBound);

  @SuppressWarnings("method.invocation.invalid") void setSeed(BaseRandom this, long seed);
}
