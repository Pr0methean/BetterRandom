package io.github.pr0methean.betterrandom.prng.adapter

import org.testng.Assert.assertEquals

import io.github.pr0methean.betterrandom.TestingDeficiency
import io.github.pr0methean.betterrandom.prng.BaseRandom
import io.github.pr0methean.betterrandom.prng.RandomTestUtils
import io.github.pr0methean.betterrandom.seed.SeedException
import org.testng.annotations.Test

class SplittableRandomAdapterTest : SingleThreadSplittableRandomAdapterTest() {

    protected override val classUnderTest: Class<out BaseRandom>
        get() = SplittableRandomAdapter::class.java

    /** SplittableRandomAdapter isn't repeatable until its seed has been specified.  */
    @Throws(SeedException::class)
    override fun testRepeatability() {
        val rng = createRng()
        rng.setSeed(BaseRandomTest.TEST_SEED)
        // Create second RNG using same seed.
        val duplicateRNG = createRng()
        duplicateRNG.setSeed(BaseRandomTest.TEST_SEED)
        RandomTestUtils.assertEquivalent(rng, duplicateRNG, 1000, "Generated sequences do not match")
    }

    /** SplittableRandomAdapter isn't repeatable until its seed has been specified.  */
    @TestingDeficiency // Failing
    @Test(enabled = false)
    @Throws(SeedException::class)
    override fun testRepeatabilityNextGaussian() {
        val rng = createRng()
        val seed = testSeedGenerator.generateSeed(getNewSeedLength(rng))
        rng.nextGaussian()
        rng.seed = seed
        // Create second RNG using same seed.
        val duplicateRNG = createRng()
        duplicateRNG.seed = seed
        assertEquals(rng.nextGaussian(), duplicateRNG.nextGaussian())
    }

    @Throws(SeedException::class)
    override fun createRng(): SplittableRandomAdapter {
        return SplittableRandomAdapter(testSeedGenerator)
    }

    /** Seeding of this PRNG is thread-local, so setSeederThread makes no sense.  */
    @Test(expectedExceptions = arrayOf(UnsupportedOperationException::class))
    @Throws(Exception::class)
    override fun testRandomSeederThreadIntegration() {
        createRng().seedGenerator = testSeedGenerator
    }

    /** Assertion-free because thread-local.  */
    @Test
    override fun testThreadSafety() {
        testThreadSafetyVsCrashesOnly(30, BaseRandomTest.FUNCTIONS_FOR_THREAD_SAFETY_TEST)
    }
}
