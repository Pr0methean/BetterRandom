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

import org.testng.Assert.assertFalse
import org.testng.Assert.assertTrue

import io.github.pr0methean.betterrandom.seed.SeedException
import java.security.NoSuchAlgorithmException
import java.util.Random
import javax.crypto.Cipher
import org.testng.SkipException
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Parameters
import org.testng.annotations.Test

/**
 * Unit test for the AES RNG.
 * @author Daniel Dyer
 * @author Chris Hennick
 */
@Test(testName = "AesCounterRandom")
open class AesCounterRandomTest : SeekableRandomTest() {

    protected var seedSizeBytes: Int = 0

    protected override val classUnderTest: Class<out BaseRandom>
        get() = AesCounterRandom::class.java

    /**
     * It'd be more elegant to use a `@Factory` static method to set the seed size (which could
     * then be a final field), but that doesn't seem to be compatible with PowerMock; see
     * https://github.com/powermock/powermock/issues/925
     *
     * @param seedSize XML parameter
     */
    @Parameters("seedSize")
    @BeforeClass
    fun setSeedSize(seedSize: Int) {
        if (seedSize > MAX_SIZE) {
            assertFalse(seedSize <= 32, "Can't handle a 32-byte seed")
            throw SkipException("Jurisdiction policy files don't allow this crypto strength")
        }
        seedSizeBytes = seedSize
    }

    override fun getNewSeedLength(basePrng: BaseRandom): Int {
        return seedSizeBytes
    }

    @Test(timeOut = 15000, expectedExceptions = arrayOf(IllegalArgumentException::class))
    @Throws(SeedException::class)
    override fun testSeedTooLong() {
        if (seedSizeBytes > 16) {
            throw SkipException("Skipping a redundant test")
        }
        createRng(
                testSeedGenerator.generateSeed(49)) // Should throw an exception.
    }

    @Test(enabled = false)
    @Throws(SeedException::class)
    override fun testRepeatabilityNextGaussian() {
        // No-op: can't be tested because setSeed merges with the existing seed
    }

    @Test(timeOut = 40000)
    @Throws(SeedException::class)
    override fun testSetSeedAfterNextLong() {
        // can't use a real SeedGenerator since we need longs, so use a Random
        val masterRNG = Random()
        val seeds = longArrayOf(masterRNG.nextLong(), masterRNG.nextLong(), masterRNG.nextLong(), masterRNG.nextLong())
        val otherSeed = masterRNG.nextLong()
        val rngs = arrayOf(AesCounterRandom(16), AesCounterRandom(16))
        for (i in 0..1) {
            for (seed in seeds) {
                val originalSeed = rngs[i].getSeed()
                assertTrue(originalSeed.size >= 16, "getSeed() returned seed that was too short")
                val rngReseeded = AesCounterRandom(originalSeed)
                val rngReseededOther = AesCounterRandom(originalSeed)
                rngReseeded.setSeed(seed)
                rngReseededOther.setSeed(otherSeed)
                assert(rngs[i] != rngReseeded)
                assert(rngReseededOther != rngReseeded)
                assert(rngs[i].nextLong() != rngReseeded.nextLong()) { "setSeed had no effect" }
                rngs[i] = rngReseeded
            }
        }
        assert(rngs[0].nextLong() != rngs[1].nextLong()) { "RNGs converged after 4 setSeed calls" }
    }

    @Test(enabled = false)
    override fun testSetSeedAfterNextInt() {
        // No-op.
    }

    @Test(timeOut = 15000)
    fun testMaxSeedLengthOk() {
        if (seedSizeBytes > 16) {
            throw SkipException("Skipping a redundant test")
        }
        assert(AesCounterRandom.maxKeyLengthBytes >= 16) { "Should allow a 16-byte key" }
        assert(AesCounterRandom.maxKeyLengthBytes <= 32) { "Shouldn't allow a key longer than 32 bytes" }
    }

    @Throws(SeedException::class)
    override fun createRng(): BaseRandom {
        return AesCounterRandom(testSeedGenerator.generateSeed(seedSizeBytes))
    }

    @Throws(SeedException::class)
    override fun createRng(seed: ByteArray?): BaseRandom {
        return AesCounterRandom(seed)
    }

    companion object {

        private val MAX_SIZE: Int

        init {
            try {
                MAX_SIZE = Cipher.getMaxAllowedKeyLength("AES") / 8 + AesCounterRandom.COUNTER_SIZE_BYTES
            } catch (e: NoSuchAlgorithmException) {
                throw AssertionError(e)
            }

        }
    }
}
