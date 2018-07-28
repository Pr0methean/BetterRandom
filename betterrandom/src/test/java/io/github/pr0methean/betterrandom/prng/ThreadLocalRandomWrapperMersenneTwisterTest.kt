package io.github.pr0methean.betterrandom.prng

import io.github.pr0methean.betterrandom.seed.SeedException
import io.github.pr0methean.betterrandom.seed.SeedGenerator
import java.io.Serializable
import java.util.function.Function
import java.util.function.Supplier
import org.testng.annotations.Test

@Test(testName = "ThreadLocalRandomWrapper:MersenneTwisterRandom")
open class ThreadLocalRandomWrapperMersenneTwisterTest : ThreadLocalRandomWrapperTest() {

    private val mtSupplier: Supplier<out BaseRandom>

    init {
        // Must be done first, or else lambda won't be serializable.
        val seedGenerator = testSeedGenerator

        mtSupplier = { MersenneTwisterRandom(seedGenerator) } as Serializable
    }

    override fun constructorParams(): Map<Class<*>, Any> {
        val params = super.constructorParams()
        params[Supplier<*>::class.java] = mtSupplier
        params[Function<*, *>::class.java] = Function<ByteArray, BaseRandom> { MersenneTwisterRandom(it) }
        return params
    }

    @Throws(SeedException::class)
    override fun createRng(): BaseRandom {
        return ThreadLocalRandomWrapper(mtSupplier)
    }
}
