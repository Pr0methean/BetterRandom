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

import com.google.common.collect.ImmutableList
import io.github.pr0methean.betterrandom.seed.SeedException
import java.util.Collections
import java.util.Random
import org.testng.annotations.Test

/**
 * Unit test for the JDK RNG.
 * @author Daniel Dyer
 * @author Chris Hennick
 */
@Test(testName = "RandomWrapper")
class RandomWrapperRandomTest : BaseRandomTest() {

    protected override val classUnderTest: Class<out BaseRandom>
        get() = RandomWrapper::class.java

    public override fun constructorParams(): Map<Class<*>, Any> {
        val params = super.constructorParams()
        params[Random::class.java] = Random()
        return params
    }

    /**
     * Assertion-free with respect to the long/double methods because, contrary to its contract to be
     * thread-safe, [Random.nextLong] is not transactional. Rather, it uses two calls to
     * [Random.next] that can interleave with calls from other threads.
     */
    override fun testThreadSafety() {
        testThreadSafety(ImmutableList.of(BaseRandomTest.NEXT_INT), emptyList())
        testThreadSafetyVsCrashesOnly(30,
                ImmutableList.of(BaseRandomTest.NEXT_LONG, BaseRandomTest.NEXT_INT, BaseRandomTest.NEXT_DOUBLE, BaseRandomTest.NEXT_GAUSSIAN, SET_WRAPPED))
    }

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

    /**
     * Test to ensure that two distinct RNGs with the same seed return the same sequence of numbers.
     */
    @Test(timeOut = 30000)
    @Throws(SeedException::class)
    override fun testRepeatability() {
        // Create an RNG using the default seeding strategy.
        val rng = RandomWrapper(testSeedGenerator)
        // Create second RNG using same seed.
        val duplicateRNG = RandomWrapper(rng.seed)
        RandomTestUtils.assertEquivalent(rng, duplicateRNG, 200,
                "Generated sequences do not match.")
    }

    @Throws(SeedException::class)
    override fun createRng(): BaseRandom {
        return RandomWrapper(testSeedGenerator)
    }

    @Throws(SeedException::class)
    override fun createRng(seed: ByteArray?): BaseRandom {
        return RandomWrapper(seed)
    }

    companion object {

        private val SET_WRAPPED = BaseRandomTest.NamedFunction<Random, Double>({ random ->
            (random as RandomWrapper).wrapped = Random()
            0.0
        }, "setWrapped")
    }
}
