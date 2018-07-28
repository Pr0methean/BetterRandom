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
import java.util.Random

/**
 *
 * Very fast pseudo random number generator.  See [this
 * page](http://school.anhb.uwa.edu.au/personalpages/kwessen/shared/Marsaglia03.html) ([archive](http://web.archive.org/web/20170313200403/school.anhb.uwa.edu.au/personalpages/kwessen/shared/Marsaglia03.html))
 * for a description.  This RNG has a period of about 2<sup>160</sup>, which is not as long as the
 * [MersenneTwisterRandom] but it is faster.
 *
 **NOTE: Because instances of this class
 * require 160-bit seeds, it is not possible to seed this RNG using the [.setSeed]
 * method inherited from [Random].  Calls to this method will have no effect. Instead the
 * seed
 * must be set by a constructor.*
 * @author Daniel Dyer
 * @since 1.2
 */
class XorShiftRandom
/**
 * Creates an RNG and seeds it with the specified seed data.
 * @param seed 20 bytes of seed data used to initialise the RNG.
 */
(seed: ByteArray) : BaseRandom(seed) {

    // Previously used an array for state but using separate fields proved to be
    // faster.
    private var state1: Int = 0
    private var state2: Int = 0
    private var state3: Int = 0
    private var state4: Int = 0
    private var state5: Int = 0

    override val newSeedLength: Int
        get() = SEED_SIZE_BYTES

    /**
     * Seed the RNG using the provided seed generation strategy.
     * @param seedGenerator The seed generation strategy that will provide the seed value for this
     * RNG.
     * @throws SeedException if there is a problem generating a seed.
     */
    @Throws(SeedException::class)
    @JvmOverloads constructor(seedGenerator: SeedGenerator = DefaultSeedGenerator.DEFAULT_SEED_GENERATOR) : this(seedGenerator.generateSeed(SEED_SIZE_BYTES)) {
    }

    override fun addSubclassFields(original: ToStringHelper): ToStringHelper {
        return original.add("state1", state1).add("state2", state2).add("state3", state3)
                .add("state4", state4).add("state5", state5)
    }

    /**
     * Reseeds this PRNG using the [DefaultSeedGenerator], since it needs a longer seed.
     * @param seed ignored
     */
    override fun setSeed(seed: Long) {
        fallbackSetSeedIfInitialized()
    }

    override fun setSeedInternal(seed: ByteArray?) {
        if (seed!!.size != SEED_SIZE_BYTES) {
            throw IllegalArgumentException("XorShiftRandom requires a 20-byte seed")
        }
        super.setSeedInternal(seed)
        val state = BinaryUtils.convertBytesToInts(this.seed!!)
        state1 = state[0]
        state2 = state[1]
        state3 = state[2]
        state4 = state[3]
        state5 = state[4]
    }

    override fun next(bits: Int): Int {
        lock.lock()
        try {
            val t = state1 xor (state1 shr 7)
            state1 = state2
            state2 = state3
            state3 = state4
            state4 = state5
            state5 = state5 xor (state5 shl 6) xor (t xor (t shl 13))
            val value = (state2 + state2 + 1) * state5
            return value.ushr(32 - bits)
        } finally {
            lock.unlock()
        }
    }

    companion object {

        private val serialVersionUID = 952521144304194886L
        private val SEED_SIZE_BYTES = 20 // Needs 5 32-bit integers.
    }
}
/**
 * Creates a new RNG and seeds it using the [DefaultSeedGenerator].
 * @throws SeedException if the [DefaultSeedGenerator] fails to generate a seed.
 */
