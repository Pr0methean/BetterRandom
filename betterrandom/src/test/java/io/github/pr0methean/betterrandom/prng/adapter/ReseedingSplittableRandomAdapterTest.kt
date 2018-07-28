package io.github.pr0methean.betterrandom.prng.adapter

import io.github.pr0methean.betterrandom.TestUtils.assertGreaterOrEqual
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator.DEFAULT_SEED_GENERATOR
import org.testng.Assert.assertEquals
import org.testng.Assert.assertNotEquals

import io.github.pr0methean.betterrandom.CloneViaSerialization
import io.github.pr0methean.betterrandom.prng.BaseRandom
import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode
import io.github.pr0methean.betterrandom.seed.FakeSeedGenerator
import io.github.pr0methean.betterrandom.seed.RandomSeederThread
import io.github.pr0methean.betterrandom.seed.SeedException
import io.github.pr0methean.betterrandom.seed.SeedGenerator
import io.github.pr0methean.betterrandom.util.BinaryUtils
import java.util.Arrays
import org.testng.annotations.Test

class ReseedingSplittableRandomAdapterTest : SingleThreadSplittableRandomAdapterTest() {

    protected override val entropyCheckMode: EntropyCheckMode
        get() = EntropyCheckMode.LOWER_BOUND

    protected override val classUnderTest: Class<out BaseRandom>
        get() = ReseedingSplittableRandomAdapter::class.java

    @Throws(SeedException::class)
    override fun createRng(): ReseedingSplittableRandomAdapter {
        return ReseedingSplittableRandomAdapter.defaultInstance
    }

    // FIXME: Why does this need more time than other PRNGs?!
    @Test(timeOut = 120000)
    @Throws(SeedException::class)
    override fun testDistribution() {
        super.testDistribution()
    }

    // FIXME: Why does this need more time than other PRNGs?!
    @Test(timeOut = 120000)
    @Throws(SeedException::class)
    override fun testIntegerSummaryStats() {
        super.testIntegerSummaryStats()
    }

    @Test
    @Throws(SeedException::class)
    override fun testSerializable() {
        val adapter = createRng()
        assertEquals(adapter, CloneViaSerialization.clone<BaseSplittableRandomAdapter>(adapter))
    }

    @Test(enabled = false)
    override fun testRepeatability() {
        // No-op.
    }

    @Test(enabled = false)
    override fun testRepeatabilityNextGaussian() {
        // No-op.
    }

    @Test
    override fun testReseeding() {
        val rng = createRng()
        val oldSeed = rng.seed
        while (rng.entropyBits > java.lang.Long.SIZE) {
            rng.nextLong()
        }
        try {
            var newSeed: ByteArray
            do {
                rng.nextBoolean()
                Thread.sleep(10)
                newSeed = rng.seed
            } while (Arrays.equals(newSeed, oldSeed))
            assertGreaterOrEqual(rng.entropyBits, newSeed.size * 8L - 1)
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }

    }

    /** Test for crashes only, since setSeed is a no-op.  */
    @Test
    @Throws(SeedException::class)
    override fun testSetSeedAfterNextLong() {
        val prng = createRng()
        prng.nextLong()
        prng.seed = testSeedGenerator.generateSeed(8)
        prng.setSeed(BinaryUtils.convertBytesToLong(testSeedGenerator.generateSeed(8)))
        prng.nextLong()
    }

    /** Test for crashes only, since setSeed is a no-op.  */
    @Test
    @Throws(SeedException::class)
    override fun testSetSeedAfterNextInt() {
        val prng = createRng()
        prng.nextInt()
        prng.seed = testSeedGenerator.generateSeed(8)
        prng.setSeed(BinaryUtils.convertBytesToLong(testSeedGenerator.generateSeed(8)))
        prng.nextInt()
    }

    /** Assertion-free since reseeding may cause divergent output.  */
    @Test(timeOut = 10000)
    override fun testSetSeedLong() {
        createRng().setSeed(0x0123456789ABCDEFL)
    }

    /**
     * This class manages its own interaction with a RandomSeederThread, so setSeederThread makes no
     * sense.
     */
    @Test(expectedExceptions = arrayOf(UnsupportedOperationException::class))
    @Throws(Exception::class)
    override fun testRandomSeederThreadIntegration() {
        createRng().seedGenerator = testSeedGenerator
    }

    @Test(enabled = false)
    override fun testSeedTooShort() {
        // No-op.
    }

    @Test(enabled = false)
    override fun testSeedTooLong() {
        // No-op.
    }

    @Test
    @Throws(SeedException::class)
    override fun testDump() {
        assertNotEquals(ReseedingSplittableRandomAdapter.getInstance(DEFAULT_SEED_GENERATOR).dump(),
                ReseedingSplittableRandomAdapter.getInstance(testSeedGenerator).dump())
    }

    @Test
    @Throws(SeedException::class)
    fun testFinalize() {
        val generator = FakeSeedGenerator()
        ReseedingSplittableRandomAdapter.getInstance(generator)
        try {
            Runtime.getRuntime().runFinalization()
        } finally {
            System.gc()
            RandomSeederThread.stopIfEmpty(generator)
        }
    }

    /** Assertion-free because thread-local.  */
    @Test
    override fun testThreadSafety() {
        testThreadSafetyVsCrashesOnly(30, BaseRandomTest.FUNCTIONS_FOR_THREAD_SAFETY_TEST)
    }
}
