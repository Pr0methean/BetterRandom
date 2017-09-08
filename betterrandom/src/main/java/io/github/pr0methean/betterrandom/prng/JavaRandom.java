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

import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import io.github.pr0methean.betterrandom.RepeatableRandom;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import java.nio.ByteBuffer;
import java.util.Random;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

/**
 * <p>This is the default {@link Random JDK RNG} extended to implement the {@link RepeatableRandom}
 * interface (for consistency with the other RNGs in this package).</p> <p> <p>The {@link
 * MersenneTwisterRandom} should be used in preference to this class because it is statistically
 * more random and performs slightly better.</p> <p> <p><em>NOTE: Instances of this class do not use
 * the seeding mechanism inherited from {@link Random}.  Calls to the {@link #setSeed(long)} method
 * will have no effect.  Instead the seedArray must be set by a constructor.</em></p>
 *
 * @author Daniel Dyer
 */
public class JavaRandom extends Random implements RepeatableRandom, ByteArrayReseedableRandom {

  private static final long serialVersionUID = -6526304552538799385L;
  private static final int SEED_SIZE_BYTES = 8;

  private byte[] seedArray;
  private ByteBuffer seedBuffer;

  /**
   * Creates a new RNG and seeds it using the default seeding strategy.
   */
  public JavaRandom() throws SeedException {
    this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(SEED_SIZE_BYTES));
  }

  /**
   * Seed the RNG using the provided seedArray generation strategy.
   *
   * @param seedGenerator The seedArray generation strategy that will provide the seedArray value for this
   *     RNG.
   * @throws SeedException If there is a problem generating a seedArray.
   */
  public JavaRandom(final SeedGenerator seedGenerator) throws SeedException {
    this(seedGenerator.generateSeed(SEED_SIZE_BYTES));
  }

  /**
   * Creates an RNG and seeds it with the specified seedArray data.
   *
   * @param seed The seedArray data used to initialise the RNG.
   */
  public JavaRandom(final byte[] seed) {
    super(createLongSeed(seed));
    initFields();
    System.arraycopy(seed, 0,
        (/* WTF CheckerFramework?! */ Object) seedArray, 0, SEED_SIZE_BYTES);
  }

  /**
   * Helper method to convert seedArray bytes into the long value required by the super class.
   */
  private static long createLongSeed(final byte[] seed) {
    if (seed == null || seed.length != SEED_SIZE_BYTES) {
      throw new IllegalArgumentException("Java RNG requires a 64-bit (8-byte) seedArray.");
    }
    return BinaryUtils.convertBytesToLong(seed, 0);
  }

  @EnsuresNonNull({"seedBuffer", "seedArray"})
  private void initFields(@UnknownInitialization JavaRandom this) {
    if (seedBuffer == null || seedArray == null) {
      seedArray = new byte[SEED_SIZE_BYTES];
      seedBuffer = ByteBuffer.wrap(seedArray);
    }
  }

  /**
   * {@inheritDoc}
   */
  public byte[] getSeed() {
    return seedArray.clone();
  }

  @Override
  public void setSeed(@UnknownInitialization(Random.class) JavaRandom this, long seed) {
    super.setSeed(seed);
    initFields();
    seedBuffer.putLong(seed);
  }

  @Override
  public void setSeed(@UnknownInitialization(Random.class) JavaRandom this, byte[] seed) {
    if (seedBuffer == null || seedArray == null) {
      seedArray = seed.clone();
      seedBuffer = ByteBuffer.wrap(seedArray);
    } else {
      System.arraycopy(seed, 0, seedArray, 0, SEED_SIZE_BYTES);
    }
    super.setSeed(seedBuffer.getLong(0));
  }

  @Override
  public boolean preferSeedWithLong() {
    return true;
  }

  @Override
  public int getNewSeedLength() {
    return SEED_SIZE_BYTES;
  }
}
