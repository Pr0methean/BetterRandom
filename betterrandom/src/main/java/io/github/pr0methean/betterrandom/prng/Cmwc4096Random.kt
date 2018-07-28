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
import io.github.pr0methean.betterrandom.util.EntryPoint
import java.util.Arrays
import java.util.Random

/**
 *
 * A Java version of George Marsaglia's [Complementary
 * Multiply With Carry (CMWC) RNG](http://school.anhb.uwa.edu.au/personalpages/kwessen/shared/Marsaglia03.html). This is a very fast PRNG with an extremely long period
 * (2<sup>131104</sup>). It should be used in preference to the [MersenneTwisterRandom] when
 * a
 * very long period is required.
 *
 *One potential drawback of this RNG is that it requires
 * significantly more seed data than the other RNGs provided by Uncommons Maths.  It requires just
 * over 16 kilobytes, which may be a problem if your are obtaining seed data from a slow or limited
 * entropy source. In contrast, the Mersenne Twister requires only 128 bits of seed data.
 *
 * *NOTE: Because instances of this class require 16-kilobyte seeds, it is not possible to
 * seed this RNG using the [.setSeed] method inherited from [Random].  Calls to
 * this method will have no effect. Instead the seed must be set by a constructor.*
 * @author Daniel Dyer
 * @since 1.2
 */
class Cmwc4096Random
/**
 * Creates an RNG and seeds it with the specified seed data.
 * @param seed 16384 bytes of seed data used to initialise the RNG.
 */
@JvmOverloads constructor(seed: ByteArray = DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(SEED_SIZE_BYTES)) : BaseRandom(seed) {

    private var state: IntArray? = null
    private var carry: Int = 0
    private var index: Int = 0

    override val seed: ByteArray
        get() = seed_!!.clone()

    /** Returns the only supported seed length.  */
    override val newSeedLength: Int
        get() = SEED_SIZE_BYTES

    /**
     * Seed the RNG using the provided seed generation strategy.
     * @param seedGenerator The seed generation strategy that will provide the seed value for this
     * RNG.
     * @throws SeedException If there is a problem generating a seed.
     */
    @EntryPoint @Throws(SeedException::class)
    constructor(seedGenerator: SeedGenerator) : this(seedGenerator.generateSeed(SEED_SIZE_BYTES))

    override fun addSubclassFields(original: ToStringHelper): ToStringHelper {
        return original.add("state", Arrays.toString(state))
    }

    /**
     * Reseeds this PRNG using the [DefaultSeedGenerator], since it needs a longer seed.
     * @param seed ignored
     */
    override fun setSeed(seed: Long) {
        fallbackSetSeedIfInitialized()
    }

    override fun setSeedInternal(seed: ByteArray?) {
        if (seed == null || seed.size != SEED_SIZE_BYTES) {
            throw IllegalArgumentException("CMWC RNG requires 16kb of seed data.")
        }
        super.setSeedInternal(seed)
        state = BinaryUtils.convertBytesToInts(seed)
        carry = 362436 // TODO: This should be randomly generated.
        index = 4095
    }

    override fun next(bits: Int): Int {
        lock.lock()
        try {
            index = index + 1 and 4095
            val t = A * (state!![index].toLong() and 0xFFFFFFFFL) + carry
            carry = (t shr 32).toInt()
            var x = t.toInt() + carry
            if (x < carry) {
                x++
                carry++
            }
            state!![index] = -0x2 - x
            return state!![index].ushr(32 - bits)
        } finally {
            lock.unlock()
        }
    }

    companion object {

        private const val SEED_SIZE_BYTES = 16384 // Needs 4,096 32-bit integers.

        private const val A = 18782L
        private const val serialVersionUID = 1731465909906078875L
    }
}
/**
 * Creates a new RNG and seeds it using the default seeding strategy.
 * @throws SeedException if any.
 */
