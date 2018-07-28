package io.github.pr0methean.betterrandom.prng.adapter

import io.github.pr0methean.betterrandom.prng.RandomTestUtils.assertEquivalent

import io.github.pr0methean.betterrandom.CloneViaSerialization
import io.github.pr0methean.betterrandom.TestingDeficiency
import io.github.pr0methean.betterrandom.prng.BaseRandom
import io.github.pr0methean.betterrandom.prng.BaseRandomTest
import io.github.pr0methean.betterrandom.seed.SeedException
import org.testng.annotations.Test

open class SingleThreadSplittableRandomAdapterTest : BaseRandomTest() {

    protected override val classUnderTest: Class<out BaseRandom>
        get() = SingleThreadSplittableRandomAdapter::class.java

    @Test(enabled = false)
    override fun testThreadSafety() {
        // No-op because this class isn't thread-safe.
    }

    /**
     * {@inheritDoc} Overridden in subclasses, so that subclassing the test can test the subclasses.
     */
    @Throws(SeedException::class)
    override fun createRng(): BaseSplittableRandomAdapter {
        return SingleThreadSplittableRandomAdapter(testSeedGenerator)
    }

    @Throws(SeedException::class)
    override fun createRng(seed: ByteArray?): BaseRandom {
        val adapter = createRng()
        adapter.seed = seed
        return adapter
    }

    @Test
    @Throws(Exception::class)
    fun testGetSplittableRandom() {
        // TODO
    }

    @TestingDeficiency
    @Test
    @Throws(SeedException::class)
    override fun testSerializable() {
        val adapter = createRng()
        // May change when serialized and deserialized, but deserializing twice should yield same object
        // and deserialization should be idempotent
        val adapter2 = CloneViaSerialization.clone(adapter)
        val adapter3 = CloneViaSerialization.clone(adapter)
        val adapter4 = CloneViaSerialization.clone(adapter2)
        assertEquivalent(adapter2, adapter3, 20,
                "Deserializing twice doesn't yield same object")
        // FIXME Failing: assertEquivalent(adapter2, adapter4, 20,
        //     "Serialization round-trip is not idempotent");
    }

    @Throws(SeedException::class)
    override fun testSetSeedLong() {
        val rng = createRng()
        val rng2 = createRng()
        rng.nextLong() // ensure they won't both be in initial state before reseeding
        rng.setSeed(0x0123456789ABCDEFL)
        rng2.setSeed(0x0123456789ABCDEFL)
        assertEquivalent(rng, rng2, 20, "Output mismatch after reseeding with same seed")
    }

    @Test
    override fun testNullSeed() {
        // No-op.
    }

    @Test(enabled = false)
    override fun testEquals() {
        // No-op.
    }

    @Test(enabled = false)
    override fun testHashCode() {
        // No-op.
    }
}
