package io.github.pr0methean.betterrandom.prng

import io.github.pr0methean.betterrandom.seed.SeedException
import org.testng.annotations.Test

@Test(testName = "Pcg64Random")
class Pcg64RandomTest : SeekableRandomTest() {

    protected override val classUnderTest: Class<out BaseRandom>
        get() = Pcg64Random::class.java

    @Throws(SeedException::class)
    override fun testSetSeedLong() {
        val rng = createRng()
        val rng2 = createRng()
        rng.nextLong() // ensure they won't both be in initial state before reseeding
        rng.setSeed(0x0123456789ABCDEFL)
        rng2.setSeed(0x0123456789ABCDEFL)
        RandomTestUtils.assertEquivalent(rng, rng2, 20,
                "Output mismatch after reseeding with same seed")
    }

    override fun createRng(): Pcg64Random {
        return Pcg64Random(testSeedGenerator)
    }

    override fun createRng(seed: ByteArray?): Pcg64Random {
        return Pcg64Random(seed)
    }
}
