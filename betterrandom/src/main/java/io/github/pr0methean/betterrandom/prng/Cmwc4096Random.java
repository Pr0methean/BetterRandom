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
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.util.Arrays;
import java.util.Random;

/**
 * <p>A Java version of George Marsaglia's <a href="http://school.anhb.uwa.edu.au/personalpages/kwessen/shared/Marsaglia03.html">Complementary
 * Multiply With Carry (CMWC) RNG</a>. This is a very fast PRNG with an extremely long period
 * (2<sup>131104</sup>). It should be used in preference to the {@link MersenneTwisterRandom} when
 * a
 * very long period is required.</p> <p>One potential drawback of this RNG is that it requires
 * significantly more seed data than the other RNGs provided by Uncommons Maths.  It requires just
 * over 16 kilobytes, which may be a problem if your are obtaining seed data from a slow or limited
 * entropy source. In contrast, the Mersenne Twister requires only 128 bits of seed data.</p>
 * <p><em>NOTE: Because instances of this class require 16-kilobyte seeds, it is not possible to
 * seed this RNG using the {@link #setSeed(long)} method inherited from {@link Random}.  Calls to
 * this method will have no effect. Instead the seed must be set by a constructor.</em></p>
 * @author Daniel Dyer
 * @since 1.2
 */
public class Cmwc4096Random extends BaseRandom {

  private static final int SEED_SIZE_BYTES = 16384; // Needs 4,096 32-bit integers.

  private static final long A = 18782L;
  private static final long serialVersionUID = 1731465909906078875L;

  private int[] state;
  private int carry;
  private int index;

  /**
   * Creates a new RNG and seeds it using the default seeding strategy.
   * @throws SeedException if any.
   */
  public Cmwc4096Random() throws SeedException {
    this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(SEED_SIZE_BYTES));
  }

  /**
   * Seed the RNG using the provided seed generation strategy.
   * @param seedGenerator The seed generation strategy that will provide the seed value for this
   *     RNG.
   * @throws SeedException If there is a problem generating a seed.
   */
  @EntryPoint public Cmwc4096Random(final SeedGenerator seedGenerator) throws SeedException {
    this(seedGenerator.generateSeed(SEED_SIZE_BYTES));
  }

  /**
   * Creates an RNG and seeds it with the specified seed data.
   * @param seed 16384 bytes of seed data used to initialize the RNG.
   */
  public Cmwc4096Random(final byte[] seed) {
    super(seed);
  }

  @Override protected ToStringHelper addSubclassFields(final ToStringHelper original) {
    return original.add("state", Arrays.toString(state));
  }

  /**
   * Reseeds this PRNG using the {@link DefaultSeedGenerator}, since it needs a longer seed.
   * @param seed ignored
   */
  @Override public void setSeed(final long seed) {
    fallbackSetSeedIfInitialized();
  }

  @Override protected void setSeedInternal(final byte[] seed) {
    super.setSeedInternal(seed);
    state = BinaryUtils.convertBytesToInts(seed);
    carry = 362436; // TODO: This should be randomly generated.
    index = 4095;
  }

  @SuppressWarnings("NumericCastThatLosesPrecision") @Override protected int next(final int bits) {
    lock.lock();
    try {
      index = (index + 1) & 4095;
      final long t = (A * (state[index] & 0xFFFFFFFFL)) + carry;
      carry = (int) (t >> 32);
      int x = ((int) t) + carry;
      if (x < carry) {
        x++;
        carry++;
      }
      state[index] = 0xFFFFFFFE - x;
      return state[index] >>> (32 - bits);
    } finally {
      lock.unlock();
    }
  }

  /** Returns the only supported seed length. */
  @Override public int getNewSeedLength() {
    return SEED_SIZE_BYTES;
  }
}
