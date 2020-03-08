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
import java.util.Arrays;
import java.util.Random;

/**
 * <p>Random number generator based on the
 * <a href="http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/emt.html"
 * target="_top">Mersenne Twister</a> algorithm developed by Makoto Matsumoto and Takuji
 * Nishimura.</p> <p>This is a very fast random number generator with good statistical properties
 * (it passes the full DIEHARD suite).  This is the best RNG for most experiments.  If a non-linear
 * generator is required, use the slower {@link AesCounterRandom} RNG.</p> <p>This PRNG is
 * deterministic, which can be advantageous for testing purposes since the output is repeatable.
 * If
 * multiple instances of this class are created with the same seed they will all have identical
 * output.</p> <p>This code is translated from the original C version and assumes that we will
 * always seed from an array of bytes.  I don't pretend to know the meanings of the magic numbers
 * or
 * how it works, it just does.</p> <p><em>NOTE: Because instances of this class require 128-bit
 * seeds, it is not possible to seed this RNG using the {@link #setSeed(long)} method inherited
 * from
 * {@link Random}.  Calls to this method will have no effect. Instead the seed must be set by a
 * constructor.</em></p>
 *
 * @author Makoto Matsumoto and Takuji Nishimura (original C version)
 * @author Daniel Dyer (Java port)
 */
public class MersenneTwisterRandom extends BaseRandom {

  private static final long serialVersionUID = -4856906677508460512L;

  // The actual seed size isn't that important, but it should be a multiple of 4.
  private static final int SEED_SIZE_BYTES = 16;

  // Magic numbers from original C version.
  private static final int N = 624;
  private static final int M = 397;
  private static final int[] MAG01 = {0, 0x9908b0df};
  private static final int UPPER_MASK = 0x80000000;
  private static final int LOWER_MASK = 0x7fffffff;
  private static final int BOOTSTRAP_SEED = 19650218;
  private static final int BOOTSTRAP_FACTOR = 1812433253;
  private static final int SEED_FACTOR1 = 1664525;
  private static final int SEED_FACTOR2 = 1566083941;
  private static final int GENERATE_MASK1 = 0x9d2c5680;
  private static final int GENERATE_MASK2 = 0xefc60000;

  private int[] mt; // State vector.
  private volatile int mtIndex = N; // Index into state vector.

  /**
   * Creates a new RNG and seeds it using the default seeding strategy.
   *
   * @throws SeedException if any.
   */
  public MersenneTwisterRandom() throws SeedException {
    this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  }

  /**
   * Creates an RNG and seeds it with the specified seed data.
   *
   * @param seed 16 bytes of seed data used to initialize the RNG.
   */
  public MersenneTwisterRandom(final byte[] seed) {
    super(seed);
  }

  /**
   * Seed the RNG using the provided seed generation strategy.
   *
   * @param seedGenerator The seed generation strategy that will provide the seed value for this
   *     RNG.
   * @throws SeedException If there is a problem generating a seed.
   */
  public MersenneTwisterRandom(final SeedGenerator seedGenerator) throws SeedException {
    this(seedGenerator.generateSeed(SEED_SIZE_BYTES));
  }

  @Override protected ToStringHelper addSubclassFields(final ToStringHelper original) {
    return original.add("mt hash", Arrays.hashCode(mt)).add("mtIndex", mtIndex);
  }

  /**
   * Reseeds this PRNG using the {@link DefaultSeedGenerator}, since it needs a longer seed.
   *
   * @param seed ignored
   */
  @Override public void setSeed(final long seed) {
    fallbackSetSeedIfInitialized();
  }

  @Override protected void initTransientFields() {
    super.initTransientFields();
    if (mt == null) {
      mt = new int[N];
    }
  }

  @Override protected void setSeedInternal(final byte[] seed) {
    super.setSeedInternal(seed);
    final int[] seedInts = BinaryUtils.convertBytesToInts(seed);

    // This section is translated from the init_genrand code in the C version.
    mt[0] = BOOTSTRAP_SEED;
    int curMtIndex; // volatile field as loop counter may hurt performance
    for (curMtIndex = 1; curMtIndex < N; curMtIndex++) {
      mt[curMtIndex] =
          ((BOOTSTRAP_FACTOR * (mt[curMtIndex - 1] ^ (mt[curMtIndex - 1] >>> 30))) + curMtIndex);
    }
    mtIndex = curMtIndex;

    // This section is translated from the init_by_array code in the C version.
    int i = 1;
    int j = 0;
    for (int k = Math.max(N, seedInts.length); k > 0; k--) {
      mt[i] = mix(mt[i], mt[i - 1], SEED_FACTOR1) + seedInts[j] + j;
      i++;
      j++;
      if (i >= N) {
        mt[0] = mt[N - 1];
        i = 1;
      }
      if (j >= seedInts.length) {
        j = 0;
      }
    }
    for (int k = N - 1; k > 0; k--) {
      mt[i] = (mix(mt[i], mt[i - 1], SEED_FACTOR2)) - i;
      i++;
      if (i >= N) {
        mt[0] = mt[N - 1];
        i = 1;
      }
    }
    mt[0] = UPPER_MASK; // Most significant bit is 1 - guarantees non-zero initial array.
  }

  private int mix(final int current, final int previous, int seedFactor) {
    return current ^ ((previous ^ (previous >>> 30)) * seedFactor);
  }

  @Override protected final int next(final int bits) {
    int y;
    lock.lock();
    try {
      final int curMtIndex;
      final int oldMtIndex = mtIndex;
      if (oldMtIndex >= N) // Generate N ints at a time.
      {
        int kk;
        for (kk = 0; kk < (N - M); kk++) {
          y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
          mt[kk] = mt[kk + M] ^ (y >>> 1) ^ MAG01[y & 0x1];
        }
        for (; kk < (N - 1); kk++) {
          y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
          mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ MAG01[y & 0x1];
        }
        y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
        mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ MAG01[y & 0x1];

        curMtIndex = 0;
      } else {
        curMtIndex = oldMtIndex;
      }
      y = mt[curMtIndex];
      mtIndex = curMtIndex + 1;
    } finally {
      lock.unlock();
    }
    // Tempering
    y ^= (y >>> 11);
    y ^= (y << 7) & GENERATE_MASK1;
    y ^= (y << 15) & GENERATE_MASK2;
    y ^= (y >>> 18);
    return y >>> (32 - bits);
  }

  /**
   * Returns the only supported seed length.
   */
  @Override public int getNewSeedLength() {
    return SEED_SIZE_BYTES;
  }
}
