package io.github.pr0methean.betterrandom.prng

import com.google.common.collect.ImmutableList
import io.github.pr0methean.betterrandom.seed.SeedException
import java.util.Random
import org.testng.annotations.Factory
import org.testng.annotations.Test

@Test(testName = "RandomWrapper:AesCounterRandom")
class RandomWrapperAesCounterRandomTest : AesCounterRandomTest() {

    protected override val classUnderTest: Class<out BaseRandom>
        get() = RandomWrapper::class.java

    init {
        seedSizeBytes = 16
    }

    @Test
    override fun testThreadSafetySetSeed() {
        testThreadSafetyVsCrashesOnly(30,
                ImmutableList.of(BaseRandomTest.NEXT_LONG, BaseRandomTest.NEXT_INT, BaseRandomTest.NEXT_DOUBLE, BaseRandomTest.NEXT_GAUSSIAN, BaseRandomTest.SET_SEED, SET_WRAPPED))
    }

    @Test(enabled = false)
    override fun testAdvanceForward() {
        // No-op: RandomWrapper isn't seekable
    }

    @Test(enabled = false)
    override fun testAdvanceBackward() {
        // No-op: RandomWrapper isn't seekable
    }

    @Test(enabled = false)
    override fun testAdvanceZero() {
        // No-op: RandomWrapper isn't seekable
    }

    @Test(enabled = false)
    override fun testAllPublicConstructors() {
        // No-op: redundant to super insofar as it works.
    }

    @Throws(SeedException::class)
    override fun createRng(): RandomWrapper {
        return RandomWrapper(AesCounterRandom(testSeedGenerator))
    }

    @Throws(SeedException::class)
    override fun createRng(seed: ByteArray?): RandomWrapper {
        return RandomWrapper(AesCounterRandom(seed))
    }

    companion object {

        private val SET_WRAPPED = BaseRandomTest.NamedFunction<Random, Double>({ random ->
            (random as RandomWrapper).wrapped = AesCounterRandom()
            0.0
        }, "setWrapped")
    }
}
