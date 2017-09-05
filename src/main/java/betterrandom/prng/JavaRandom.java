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
package betterrandom.prng;

import betterrandom.RepeatableRandom;
import betterrandom.seed.DefaultSeedGenerator;
import betterrandom.seed.SeedException;
import betterrandom.seed.SeedGenerator;
import betterrandom.util.BinaryUtils;
import java.util.Random;

/**
 * <p>This is the default {@link Random JDK RNG} extended to implement the {@link RepeatableRandom}
 * interface (for consistency with the other RNGs in this package).</p>
 * <p>
 * <p>The {@link MersenneTwisterRandom} should be used in preference to this class because it is
 * statistically more random and performs slightly better.</p>
 * <p>
 * <p><em>NOTE: Instances of this class do not use the seeding mechanism inherited from {@link
 * Random}.  Calls to the {@link #setSeed(long)} method will have no effect.  Instead the seed must
 * be set by a constructor.</em></p>
 *
 * @author Daniel Dyer
 */
public class JavaRandom extends Random implements RepeatableRandom {

  private static final long serialVersionUID = -6526304552538799385L;
  private static final int SEED_SIZE_BYTES = 8;

  private final byte[] seed;

  /**
   * Creates a new RNG and seeds it using the default seeding strategy.
   */
  public JavaRandom() throws SeedException {
    this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(SEED_SIZE_BYTES));
  }

  /**
   * Seed the RNG using the provided seed generation strategy.
   *
   * @param seedGenerator The seed generation strategy that will provide the seed value for this
   *     RNG.
   * @throws SeedException If there is a problem generating a seed.
   */
  public JavaRandom(SeedGenerator seedGenerator) throws SeedException {
    this(seedGenerator.generateSeed(SEED_SIZE_BYTES));
  }

  /**
   * Creates an RNG and seeds it with the specified seed data.
   *
   * @param seed The seed data used to initialise the RNG.
   */
  public JavaRandom(byte[] seed) {
    super(createLongSeed(seed));
    this.seed = seed.clone();
  }

  /**
   * Helper method to convert seed bytes into the long value required by the super class.
   */
  private static long createLongSeed(byte[] seed) {
    if (seed == null || seed.length != SEED_SIZE_BYTES) {
      throw new IllegalArgumentException("Java RNG requires a 64-bit (8-byte) seed.");
    }
    return BinaryUtils.convertBytesToLong(seed, 0);
  }

  /**
   * {@inheritDoc}
   */
  public byte[] getSeed() {
    return seed.clone();
  }
}
