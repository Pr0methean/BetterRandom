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
package io.github.pr0methean.betterrandom.prng

import com.google.common.base.MoreObjects.ToStringHelper
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator
import io.github.pr0methean.betterrandom.seed.SeedException
import io.github.pr0methean.betterrandom.seed.SeedGenerator
import io.github.pr0methean.betterrandom.util.BinaryUtils
import java.util.Arrays
import java.util.Random

/**
 *
 * Random number generator based on the [Mersenne Twister](http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/emt.html) algorithm developed by Makoto Matsumoto and Takuji
 * Nishimura.
 *
 *This is a very fast random number generator with good statistical properties
 * (it passes the full DIEHARD suite).  This is the best RNG for most experiments.  If a non-linear
 * generator is required, use the slower [AesCounterRandom] RNG.
 *
 *This PRNG is
 * deterministic, which can be advantageous for testing purposes since the output is repeatable.
 * If
 * multiple instances of this class are created with the same seed they will all have identical
 * output.
 *
 *This code is translated from the original C version and assumes that we will
 * always seed from an array of bytes.  I don't pretend to know the meanings of the magic numbers
 * or
 * how it works, it just does.
 *
 **NOTE: Because instances of this class require 128-bit
 * seeds, it is not possible to seed this RNG using the [.setSeed] method inherited
 * from
 * [Random].  Calls to this method will have no effect. Instead the seed must be set by a
 * constructor.*
 * @author Makoto Matsumoto and Takuji Nishimura (original C version)
 * @author Daniel Dyer (Java port)
 */
class MersenneTwisterRandom
/**
 * Creates an RNG and seeds it with the specified seed data.
 * @param seed 16 bytes of seed data used to initialise the RNG.
 */
(seed: ByteArray) : BaseRandom(seed) {

    private var mt: IntArray? = null // State vector.
    @Volatile
    private var mtIndex = N // Index into state vector.

    /** Returns the only supported seed length.  */
    override val newSeedLength: Int
        get() = SEED_SIZE_BYTES

    /**
     * Seed the RNG using the provided seed generation strategy.
     * @param seedGenerator The seed generation strategy that will provide the seed value for this
     * RNG.
     * @throws SeedException If there is a problem generating a seed.
     */
    @Throws(SeedException::class)
    @JvmOverloads constructor(seedGenerator: SeedGenerator = DefaultSeedGenerator.DEFAULT_SEED_GENERATOR) : this(seedGenerator.generateSeed(SEED_SIZE_BYTES)) {
    }

    override fun addSubclassFields(original: ToStringHelper): ToStringHelper {
        return original.add("mt hash", Arrays.hashCode(mt)).add("mtIndex", mtIndex)
    }

    /**
     * Reseeds this PRNG using the [DefaultSeedGenerator], since it needs a longer seed.
     * @param seed ignored
     */
    override fun setSeed(seed: Long) {
        fallbackSetSeedIfInitialized()
    }

    override fun initTransientFields() {
        super.initTransientFields()
        if (mt == null) {
            mt = IntArray(N)
        }
    }

    override fun setSeedInternal(seed: ByteArray?) {
        if (seed == null || seed.size != SEED_SIZE_BYTES) {
            throw IllegalArgumentException(
                    "Mersenne Twister RNG requires a 128-bit (16-byte) seed;" + " input seed is "
                            + BinaryUtils.convertBytesToHexString(seed))
        }
        super.setSeedInternal(seed)
        val seedInts = BinaryUtils.convertBytesToInts(seed)

        // This section is translated from the init_genrand code in the C version.
        mt[0] = BOOTSTRAP_SEED
        mtIndex = 1
        while (mtIndex < N) {
            mt[mtIndex] = BOOTSTRAP_FACTOR * (mt!![mtIndex - 1] xor mt!![mtIndex - 1].ushr(30)) + mtIndex
            mtIndex++
        }

        // This section is translated from the init_by_array code in the C version.
        var i = 1
        var j = 0
        for (k in Math.max(N, seedInts.size) downTo 1) {
            mt[i] = (mt!![i] xor (mt!![i - 1] xor mt!![i - 1].ushr(30)) * SEED_FACTOR1) + seedInts[j] + j
            i++
            j++
            if (i >= N) {
                mt[0] = mt!![N - 1]
                i = 1
            }
            if (j >= seedInts.size) {
                j = 0
            }
        }
        for (k in N - 1 downTo 1) {
            mt[i] = (mt!![i] xor (mt!![i - 1] xor mt!![i - 1].ushr(30)) * SEED_FACTOR2) - i
            i++
            if (i >= N) {
                mt[0] = mt!![N - 1]
                i = 1
            }
        }
        mt[0] = UPPER_MASK // Most significant bit is 1 - guarantees non-zero initial array.
    }

    override fun next(bits: Int): Int {
        var y: Int
        lock.lock()
        try {
            if (mtIndex >= N)
            // Generate N ints at a time.
            {
                var kk: Int
                kk = 0
                while (kk < N - M) {
                    y = mt!![kk] and UPPER_MASK or (mt!![kk + 1] and LOWER_MASK)
                    mt[kk] = mt!![kk + M] xor y.ushr(1) xor MAG01[y and 0x1]
                    kk++
                }
                while (kk < N - 1) {
                    y = mt!![kk] and UPPER_MASK or (mt!![kk + 1] and LOWER_MASK)
                    mt[kk] = mt!![kk + (M - N)] xor y.ushr(1) xor MAG01[y and 0x1]
                    kk++
                }
                y = mt!![N - 1] and UPPER_MASK or (mt!![0] and LOWER_MASK)
                mt[N - 1] = mt!![M - 1] xor y.ushr(1) xor MAG01[y and 0x1]

                mtIndex = 0
            }

            y = mt!![mtIndex]
            mtIndex++
        } finally {
            lock.unlock()
        }
        // Tempering
        y = y xor y.ushr(11)
        y = y xor (y shl 7 and GENERATE_MASK1)
        y = y xor (y shl 15 and GENERATE_MASK2)
        y = y xor y.ushr(18)
        return y.ushr(32 - bits)
    }

    companion object {

        private val serialVersionUID = -4856906677508460512L

        // The actual seed size isn't that important, but it should be a multiple of 4.
        private val SEED_SIZE_BYTES = 16

        // Magic numbers from original C version.
        private val N = 624
        private val M = 397
        private val MAG01 = intArrayOf(0, -0x66f74f21)
        private val UPPER_MASK = -0x80000000
        private val LOWER_MASK = 0x7fffffff
        private val BOOTSTRAP_SEED = 19650218
        private val BOOTSTRAP_FACTOR = 1812433253
        private val SEED_FACTOR1 = 1664525
        private val SEED_FACTOR2 = 1566083941
        private val GENERATE_MASK1 = -0x62d3a980
        private val GENERATE_MASK2 = -0x103a0000
    }
}
/**
 * Creates a new RNG and seeds it using the default seeding strategy.
 * @throws SeedException if any.
 */
