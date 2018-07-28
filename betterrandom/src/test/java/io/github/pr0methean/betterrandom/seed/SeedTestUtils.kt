package io.github.pr0methean.betterrandom.seed

import org.testng.Assert.assertFalse

import java.util.Arrays

internal enum class SeedTestUtils {
    ;

    companion object {

        val SEED_SIZE = 16
        private val ALL_ZEROES = ByteArray(SEED_SIZE)

        @Throws(SeedException::class)
        fun testGenerator(seedGenerator: SeedGenerator) {
            val seed = seedGenerator.generateSeed(SEED_SIZE)
            assert(seed.size == SEED_SIZE) { "Failed to generate seed of correct length" }
            assertFalse(Arrays.equals(seed, ALL_ZEROES))
            val secondSeed = ByteArray(SEED_SIZE)
            seedGenerator.generateSeed(secondSeed) // Check that other syntax also works
            assertFalse(Arrays.equals(secondSeed, ALL_ZEROES))
            assertFalse(Arrays.equals(seed, secondSeed))
        }
    }
}
