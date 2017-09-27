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
import io.github.pr0methean.betterrandom.EntropyCountingRandom;
import io.github.pr0methean.betterrandom.RepeatableRandom;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.EntryPoint;
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

  @SuppressWarnings("PublicStaticArrayField")
  public static final byte[] DUMMY_SEED = new byte[8];
  private static final long serialVersionUID = -6526304552538799385L;
  private static final int SEED_SIZE_BYTES = 8;
  private Random wrapped;
  private boolean unknownSeed = true;

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
  @EntryPoint
  public RandomWrapper(final SeedGenerator seedGenerator) throws SeedException {
    super(seedGenerator, SEED_SIZE_BYTES);
    wrapped = new Random(longSeedBuffer.getLong(0));
    unknownSeed = false;
  }

  /**
   * Creates an RNG and seeds it with the specified seedArray data.
   *
   * @param seed The seedArray data used to initialise the RNG.
   */
  public RandomWrapper(final byte[] seed) {
    super(seed);
    wrapped = new Random(longSeedBuffer.getLong(0));
    unknownSeed = false;
  }

  /**
   * Creates an instance wrapping the given {@link Random}.
   *
   * @param wrapped The {@link Random} to wrap.
   */
  @EntryPoint
  public RandomWrapper(final Random wrapped) {
    super(getSeedOrDummy(wrapped)); // We won't know the wrapped PRNG's seed
    unknownSeed = !(wrapped instanceof RepeatableRandom);
    readEntropyOfWrapped(wrapped);
    this.wrapped = wrapped;
  }

  private static byte[] getSeedOrDummy(final Random wrapped) {
    return wrapped instanceof RepeatableRandom ? ((RepeatableRandom) wrapped).getSeed()
        : DUMMY_SEED;
  }

  /** @return The wrapped {@link Random}. */
  @EntryPoint
  public Random getWrapped() {
    return wrapped;
  }

  /** @param wrapped The new {@link Random} instance to wrap. */
  @EntryPoint
  public void setWrapped(final Random wrapped) {
    this.wrapped = wrapped;
    readEntropyOfWrapped(wrapped);
    this.seed = getSeedOrDummy(wrapped);
    unknownSeed = !(wrapped instanceof RepeatableRandom);
  }

  private void readEntropyOfWrapped(
      @UnknownInitialization(BaseRandom.class)RandomWrapper this,
      final Random wrapped) {
    entropyBits.set(wrapped instanceof EntropyCountingRandom
        ? ((EntropyCountingRandom) wrapped).entropyBits()
        : 64);
  }

  @Override
  protected ToStringHelper addSubSubclassFields(final ToStringHelper original) {
    return original.add("wrapped", wrapped);
  }

  @Override
  public byte[] getSeed() {
    if (unknownSeed) {
      throw new UnsupportedOperationException();
    }
    return super.getSeed();
  }

  @Override
  public void setSeedInternal(@UnknownInitialization(Random.class)RandomWrapper this,
      final byte[] seed) {
    if (seed.length != SEED_SIZE_BYTES) {
      throw new IllegalArgumentException("RandomWrapper requires a 64-bit (8-byte) seed.");
    }
    super.setSeedInternal(seed);
    if (wrapped != null && longSeedBuffer != null && longSeedArray != null) {
      System.arraycopy(seed, 0, longSeedArray, 0, SEED_SIZE_BYTES);
      wrapped.setSeed(longSeedBuffer.getLong(0));
      unknownSeed = false;
    }
  }

  @Override
  public boolean preferSeedWithLong() {
    return true;
  }

  @Override
  public int getNewSeedLength(@UnknownInitialization RandomWrapper this) {
    return SEED_SIZE_BYTES;
  }

  public void nextBytes(final byte[] bytes) {
    wrapped.nextBytes(bytes);
    recordEntropySpent(bytes.length * 8L);
  }

  public int nextInt() {
    final int result = wrapped.nextInt();
    recordEntropySpent(32);
    return result;
  }

  public int nextInt(final int bound) {
    final int result = wrapped.nextInt(bound);
    recordEntropySpent(entropyOfInt(0, bound));
    return result;
  }

  public long nextLong() {
    final long result = wrapped.nextLong();
    recordEntropySpent(64);
    return result;
  }

  public boolean nextBoolean() {
    final boolean result = wrapped.nextBoolean();
    recordEntropySpent(1);
    return result;
  }

  public float nextFloat() {
    final float result = wrapped.nextFloat();
    recordEntropySpent(ENTROPY_OF_FLOAT);
    return result;
  }

  public double nextDouble() {
    final double result = wrapped.nextDouble();
    recordEntropySpent(ENTROPY_OF_DOUBLE);
    return result;
  }

  public double nextGaussian() {
    final double result = wrapped.nextGaussian();

    // Upper bound. 2 Gaussians are generated from 2 nextDouble calls, which once made are either
    // used or rerolled.
    recordEntropySpent(ENTROPY_OF_DOUBLE);

    return result;
  }

  public IntStream ints(final long streamSize) {
    final IntStream result = wrapped.ints(streamSize);
    recordEntropySpent(32 * streamSize);
    return result;
  }

  public IntStream ints() {
    final IntStream result = wrapped.ints();
    recordAllEntropySpent();
    return result;
  }

  public IntStream ints(final long streamSize, final int randomNumberOrigin,
      final int randomNumberBound) {
    final IntStream result = wrapped.ints(streamSize, randomNumberOrigin, randomNumberBound);
    recordEntropySpent(streamSize * entropyOfInt(randomNumberOrigin, randomNumberBound));
    return result;
  }

  public IntStream ints(final int randomNumberOrigin, final int randomNumberBound) {
    final IntStream result = wrapped.ints(randomNumberOrigin, randomNumberBound);
    recordAllEntropySpent();
    return result;
  }

  public LongStream longs(final long streamSize) {
    final LongStream result = wrapped.longs(streamSize);
    recordEntropySpent(64 * streamSize);
    return result;
  }

  public LongStream longs() {
    final LongStream result = wrapped.longs();
    recordAllEntropySpent();
    return result;
  }

  public LongStream longs(final long streamSize, final long randomNumberOrigin,
      final long randomNumberBound) {
    final LongStream result = wrapped.longs(streamSize, randomNumberOrigin, randomNumberBound);
    recordEntropySpent(streamSize * entropyOfLong(randomNumberOrigin, randomNumberBound));
    return result;
  }

  public LongStream longs(final long randomNumberOrigin, final long randomNumberBound) {
    final LongStream result = wrapped.longs(randomNumberOrigin, randomNumberBound);
    recordAllEntropySpent();
    return result;
  }

  public DoubleStream doubles(final long streamSize) {
    final DoubleStream result = wrapped.doubles(streamSize);
    recordEntropySpent(streamSize * ENTROPY_OF_DOUBLE);
    return result;
  }

  public DoubleStream doubles() {
    final DoubleStream result = wrapped.doubles();
    recordAllEntropySpent();
    return result;
  }

  public DoubleStream doubles(final long streamSize, final double randomNumberOrigin,
      final double randomNumberBound) {
    final DoubleStream result = wrapped.doubles(streamSize, randomNumberOrigin, randomNumberBound);
    recordEntropySpent(streamSize * ENTROPY_OF_DOUBLE);
    return result;
  }

  public DoubleStream doubles(final double randomNumberOrigin, final double randomNumberBound) {
    final DoubleStream result = wrapped.doubles(randomNumberOrigin, randomNumberBound);
    recordAllEntropySpent();
    return result;
  }
}
