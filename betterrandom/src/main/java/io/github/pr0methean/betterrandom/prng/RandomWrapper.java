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

import static org.checkerframework.checker.nullness.NullnessUtil.castNonNull;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import io.github.pr0methean.betterrandom.RepeatableRandom;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.util.Random;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

/**
 * <p>Wraps any {@link Random} as a {@link RepeatableRandom} and {@link ByteArrayReseedableRandom}.
 * Can be used to encapsulate away a change of implementation in midstream. Note that when this is
 * constructed using an existing instance, and after {@link #setWrapped(Random)} is called, we won't
 * know the initial seed until the next {@link #setSeed(byte[])} or {@link #setSeed(long)} call, and
 * so {@link #getSeed()} will return an empty array until then.</p>
 *
 * @author Chris Hennick
 * @version $Id: $Id
 */
public class RandomWrapper extends BaseRandom {

  private static final long serialVersionUID = -6526304552538799385L;
  private static final int SEED_SIZE_BYTES = 8;
  public static final byte[] EMPTY_ARRAY = new byte[0];

  /** @return The wrapped {@link Random}. */
  public Random getWrapped() {
    return wrapped;
  }

  /** @param wrapped The new {@link Random} instance to wrap. */
  public void setWrapped(Random wrapped) {
    this.wrapped = wrapped;
    this.seed = EMPTY_ARRAY;
  }

  private Random wrapped;

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
   */
  public RandomWrapper(Random wrapped) {
    super(EMPTY_ARRAY); // We won't know the wrapped PRNG's seed
    this.wrapped = wrapped;
  }

  @Override
  public void setSeedInternal(@UnknownInitialization(Random.class)RandomWrapper this,
      byte[] seed) {
    if (seed.length != SEED_SIZE_BYTES) {
      throw new IllegalArgumentException("RandomWrapper requires a 64-bit (8-byte) seed.");
    }
    super.setSeedInternal(seed);
    if (wrapped != null && longSeedBuffer != null && longSeedArray != null) {
      System.arraycopy(seed, 0, longSeedArray, 0, SEED_SIZE_BYTES);
      wrapped.setSeed(longSeedBuffer.getLong(0));
    }
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
