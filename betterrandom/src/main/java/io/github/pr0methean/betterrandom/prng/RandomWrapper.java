// ============================================================================
//   Copyright 2006-2012 Daniel W. Dyer
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
// ============================================================================
package io.github.pr0methean.betterrandom.prng;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import io.github.pr0methean.betterrandom.RepeatableRandom;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

/**
 * <p>Wraps any {@link Random} as a {@link RepeatableRandom} and {@link
 * ByteArrayReseedableRandom}. Note that if this is constructed using an existing instance, we won't
 * know the initial seed, and so {@link #getSeed()} will return an empty array.</p>
 *
 * @author Daniel Dyer, Chris Hennick
 * @version $Id: $Id
 */
public class RandomWrapper extends BaseRandom {

  private static final long serialVersionUID = -6526304552538799385L;
  private static final int SEED_SIZE_BYTES = 8;
  private final Random wrapped;

  /**
   * Creates a new RNG and seeds it using the default seeding strategy.
   *
   * @throws io.github.pr0methean.betterrandom.seed.SeedException if any.
   */
  public RandomWrapper() throws SeedException {
    this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(SEED_SIZE_BYTES));
  }

  /**
   * Seed the RNG using the provided seedArray generation strategy.
   *
   * @param seedGenerator The seedArray generation strategy that will provide the seedArray
   *     value for this RNG.
   * @throws io.github.pr0methean.betterrandom.seed.SeedException If there is a problem
   *     generating a seedArray.
   */
  public RandomWrapper(final SeedGenerator seedGenerator) throws SeedException {
    super(seedGenerator, SEED_SIZE_BYTES);
    wrapped = new Random(longSeedBuffer.getLong(0));
  }

  /**
   * Creates an RNG and seeds it with the specified seedArray data.
   *
   * @param seed The seedArray data used to initialise the RNG.
   */
  public RandomWrapper(final byte[] seed) {
    super(seed);
    wrapped = new Random(longSeedBuffer.getLong(0));
  }

  @Override
  public ToStringHelper addSubclassFields(ToStringHelper original) {
    return original.add("wrapped", wrapped);
  }

  /**
   * Creates an instance wrapping the given {@link Random}.
   *
   * @param wrapped The {@link Random} to wrap.
   * @throws SeedException Should never happen, but Java won't let us wrap the super() call in a try
   *     block.
   */
  public RandomWrapper(Random wrapped) throws SeedException {
    super(0); // We won't know the wrapped PRNG's seed
    this.wrapped = wrapped;
  }

  @Override
  public void setSeed(@UnknownInitialization(Random.class)RandomWrapper RandomWrapper.this, byte[] seed) {
    super.setSeed(seed);
    wrapped.setSeed(longSeedBuffer.getLong(0));
  }

  /** {@inheritDoc} */
  @Override
  public boolean preferSeedWithLong() {
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public int getNewSeedLength() {
    return SEED_SIZE_BYTES;
  }

  public void nextBytes(byte[] bytes) {
    wrapped.nextBytes(bytes);
  }

  public int nextInt() {
    return wrapped.nextInt();
  }

  public int nextInt(int bound) {
    return wrapped.nextInt(bound);
  }

  public long nextLong() {
    return wrapped.nextLong();
  }

  public boolean nextBoolean() {
    return wrapped.nextBoolean();
  }

  public float nextFloat() {
    return wrapped.nextFloat();
  }

  public double nextDouble() {
    return wrapped.nextDouble();
  }

  public double nextGaussian() {
    return wrapped.nextGaussian();
  }

  public IntStream ints(long streamSize) {
    return wrapped.ints(streamSize);
  }

  public IntStream ints() {
    return wrapped.ints();
  }

  public IntStream ints(long streamSize, int randomNumberOrigin,
      int randomNumberBound) {
    return wrapped.ints(streamSize, randomNumberOrigin, randomNumberBound);
  }

  public IntStream ints(int randomNumberOrigin, int randomNumberBound) {
    return wrapped.ints(randomNumberOrigin, randomNumberBound);
  }

  public LongStream longs(long streamSize) {
    return wrapped.longs(streamSize);
  }

  public LongStream longs() {
    return wrapped.longs();
  }

  public LongStream longs(long streamSize, long randomNumberOrigin,
      long randomNumberBound) {
    return wrapped.longs(streamSize, randomNumberOrigin, randomNumberBound);
  }

  public LongStream longs(long randomNumberOrigin, long randomNumberBound) {
    return wrapped.longs(randomNumberOrigin, randomNumberBound);
  }

  public DoubleStream doubles(long streamSize) {
    return wrapped.doubles(streamSize);
  }

  public DoubleStream doubles() {
    return wrapped.doubles();
  }

  public DoubleStream doubles(long streamSize, double randomNumberOrigin,
      double randomNumberBound) {
    return wrapped.doubles(streamSize, randomNumberOrigin, randomNumberBound);
  }

  public DoubleStream doubles(double randomNumberOrigin, double randomNumberBound) {
    return wrapped.doubles(randomNumberOrigin, randomNumberBound);
  }
}
