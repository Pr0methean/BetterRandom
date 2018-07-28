package io.github.pr0methean.betterrandom.prng

import com.google.common.collect.ImmutableList
import io.github.pr0methean.betterrandom.seed.SeedException
import io.github.pr0methean.betterrandom.seed.SeedGenerator
import java.lang.reflect.InvocationTargetException
import java.util.Random
import org.testng.annotations.Test

@Test(testName = "RandomWrapper:MersenneTwisterRandom")
class RandomWrapperMersenneTwisterRandomTest : MersenneTwisterRandomTest() {

    private val setWrapped: BaseRandomTest.NamedFunction<Random, Double>

    protected override val classUnderTest: Class<out BaseRandom>
        get() = RandomWrapper::class.java

    init {
        val seedGenerator = testSeedGenerator
        setWrapped = BaseRandomTest.NamedFunction({ random ->
            (random as RandomWrapper).wrapped = MersenneTwisterRandom(seedGenerator)
            0.0
        }, "setWrapped")
    }

    override fun testThreadSafety() {
        super.testThreadSafety()
        testThreadSafetyVsCrashesOnly(30,
                ImmutableList.of(BaseRandomTest.NEXT_LONG, BaseRandomTest.NEXT_INT, BaseRandomTest.NEXT_DOUBLE, BaseRandomTest.NEXT_GAUSSIAN, setWrapped))
    }

    @Test(enabled = false)
    @Throws(SeedException::class, IllegalAccessException::class, InstantiationException::class, InvocationTargetException::class)
    override fun testAllPublicConstructors() {
        // No-op: redundant to super insofar as it works.
    }

    @Throws(SeedException::class)
    override fun createRng(): RandomWrapper {
        return RandomWrapper(MersenneTwisterRandom(testSeedGenerator))
    }

    @Throws(SeedException::class)
    override fun createRng(seed: ByteArray?): RandomWrapper {
        return RandomWrapper(MersenneTwisterRandom(seed))
    }
}
