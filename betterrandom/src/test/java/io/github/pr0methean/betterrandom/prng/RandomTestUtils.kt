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

import org.testng.Assert.assertEquals
import org.testng.Assert.assertNotSame

import io.github.pr0methean.betterrandom.CloneViaSerialization
import io.github.pr0methean.betterrandom.TestUtils
import io.github.pr0methean.betterrandom.util.Dumpable
import java.util.HashSet
import java.util.Random
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Supplier
import java.util.stream.BaseStream
import java.util.stream.Stream
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics
import org.testng.Reporter

/**
 * Provides methods used for testing the operation of RNG implementations.
 * @author Daniel Dyer
 */
enum class RandomTestUtils {
    ;

    enum class EntropyCheckMode {
        EXACT,
        LOWER_BOUND,
        OFF
    }

    companion object {

        private val INSTANCES_TO_HASH = 25
        private val EXPECTED_UNIQUE_HASHES = (0.8 * INSTANCES_TO_HASH).toInt()

        fun checkRangeAndEntropy(prng: BaseRandom, expectedEntropySpent: Long,
                                 numberSupplier: Supplier<out Number>, origin: Double, bound: Double,
                                 entropyCheckMode: EntropyCheckMode) {
            val oldEntropy = prng.entropyBits
            val output = numberSupplier.get()
            TestUtils.assertGreaterOrEqual(output.toDouble(), origin)
            if (bound - 1.0 == bound) {
                // Can't do a strict check because of floating-point rounding
                TestUtils.assertLessOrEqual(output.toDouble(), bound)
            } else {
                TestUtils.assertLess(output.toDouble(), bound)
            }
            val entropy = prng.entropyBits
            val expectedEntropy = oldEntropy - expectedEntropySpent
            when (entropyCheckMode) {
                RandomTestUtils.EntropyCheckMode.EXACT -> assertEquals(entropy, expectedEntropy)
                RandomTestUtils.EntropyCheckMode.LOWER_BOUND -> TestUtils.assertGreaterOrEqual(entropy, expectedEntropy)
                RandomTestUtils.EntropyCheckMode.OFF -> {
                }
            }
        }

        /**
         * @param expectedCount Negative for an endless stream.
         * @param origin Minimum expected value, inclusive.
         * @param bound Maximum expected value, exclusive.
         */
        fun checkStream(prng: BaseRandom, maxEntropySpentPerNumber: Long,
                        stream: Stream<out Number>, expectedCount: Int, origin: Double,
                        bound: Double, checkEntropyCount: Boolean) {
            val entropy = AtomicLong(prng.entropyBits)
            val streamToUse = if (expectedCount < 0) stream.sequential().limit(20) else stream
            val count = streamToUse.mapToLong { number ->
                TestUtils.assertGreaterOrEqual(number.toDouble(), origin)
                TestUtils.assertLess(number.toDouble(), bound)
                if (checkEntropyCount && !streamToUse.isParallel) {
                    val newEntropy = prng.entropyBits
                    TestUtils.assertGreaterOrEqual(newEntropy,
                            entropy.getAndSet(newEntropy) - maxEntropySpentPerNumber)
                }
                1
            }.sum()
            if (expectedCount >= 0) {
                assertEquals(count, expectedCount.toLong())
            }
            if (checkEntropyCount && streamToUse.isParallel) {
                TestUtils.assertGreaterOrEqual(prng.entropyBits,
                        entropy.get() - maxEntropySpentPerNumber * count)
            }
        }

        /**
         * Test that the given parameterless constructor, called twice, doesn't produce RNGs that compare
         * as equal. Also checks for compliance with basic parts of the Object.equals() contract.
         */
        fun doEqualsSanityChecks(ctor: Supplier<out Random>) {
            val rng = ctor.get()
            val rng2 = ctor.get()
            assert(rng != rng2)
            assert(rng == rng) { "RNG doesn't compare equal to itself" }
            assert(rng != null) { "RNG compares equal to null" }
            assert(rng != Random()) { "RNG compares equal to new Random()" }
        }

        /**
         * Test that in a sample of 100 RNGs from the given parameterless constructor, there are at least
         * 90 unique hash codes.
         */
        fun testHashCodeDistribution(ctor: Supplier<out Random>): Boolean {
            val uniqueHashCodes = HashSet<Int>(INSTANCES_TO_HASH)
            for (i in 0 until INSTANCES_TO_HASH) {
                uniqueHashCodes.add(ctor.get().hashCode())
            }
            return uniqueHashCodes.size >= EXPECTED_UNIQUE_HASHES
        }

        /**
         * Test to ensure that two distinct RNGs with the same seed return the same sequence of numbers
         * and compare as equal.
         * @param rng1 The first RNG.  Its output is compared to that of `rng2`.
         * @param rng2 The second RNG.  Its output is compared to that of `rng1`.
         * @param iterations The number of values to generate from each RNG and compare.
         * @return true if the two RNGs produce the same sequence of values, false otherwise.
         */
        fun testEquivalence(rng1: Random, rng2: Random,
                            iterations: Int): Boolean {
            for (i in 0 until iterations) {
                if (rng1.nextInt() != rng2.nextInt()) {
                    return false
                }
            }
            return true
        }

        private fun assertEquivalentOrDistinct(rng1: Random, rng2: Random,
                                               iterations: Int, message: String, shouldBeEquivalent: Boolean) {
            val fullMessage = String.format("%s:%n%s%nvs.%n%s%n", message, toString(rng1), toString(rng2))
            if (testEquivalence(rng1, rng2, iterations) != shouldBeEquivalent) {
                throw AssertionError(fullMessage)
            }
        }

        fun assertEquivalent(rng1: Random, rng2: Random,
                             iterations: Int, message: String) {
            assertEquivalentOrDistinct(rng1, rng2, iterations, message, true)
        }

        fun assertDistinct(rng1: Random, rng2: Random,
                           iterations: Int, message: String) {
            assertEquivalentOrDistinct(rng1, rng2, iterations, message, false)
        }

        fun toString(rng: Random): String {
            return if (rng is Dumpable) (rng as Dumpable).dump() else rng.toString()
        }

        /**
         * This is a rudimentary check to ensure that the output of a given RNG is approximately uniformly
         * distributed.  If the RNG output is not uniformly distributed, this method will return a poor
         * estimate for the value of pi.
         * @param rng The RNG to test.
         * @param iterations The number of random points to generate for use in the calculation.  This
         * value needs to be sufficiently large in order to produce a reasonably accurate result
         * (assuming the RNG is uniform). Less than 10,000 is not particularly useful.  100,000 should
         * be sufficient.
         * @return An approximation of pi generated using the provided RNG.
         */
        private fun calculateMonteCarloValueForPi(rng: Random, iterations: Int): Double {
            // Assumes a quadrant of a circle of radius 1, bounded by a box with
            // sides of length 1.  The area of the square is therefore 1 square unit
            // and the area of the quadrant is (pi * r^2) / 4.
            var totalInsideQuadrant = 0
            // Generate the specified number of random points and count how many fall
            // within the quadrant and how many do not.  We expect the number of points
            // in the quadrant (expressed as a fraction of the total number of points)
            // to be pi/4.  Therefore pi = 4 * ratio.
            for (i in 0 until iterations) {
                val x = rng.nextDouble()
                val y = rng.nextDouble()
                if (isInQuadrant(x, y)) {
                    ++totalInsideQuadrant
                }
            }
            // From these figures we can deduce an approximate value for Pi.
            return 4 * (totalInsideQuadrant.toDouble() / iterations)
        }

        /**
         * Uses Pythagoras' theorem to determine whether the specified coordinates fall within the area of
         * the quadrant of a circle of radius 1 that is centered on the origin.
         * @param x The x-coordinate of the point (must be between 0 and 1).
         * @param y The y-coordinate of the point (must be between 0 and 1).
         * @return True if the point is within the quadrant, false otherwise.
         */
        private fun isInQuadrant(x: Double, y: Double): Boolean {
            val distance = Math.sqrt(x * x + y * y)
            return distance <= 1
        }

        /**
         * Generates a sequence of integers from a given random number generator and then calculates the
         * standard deviation of the sample.
         * @param rng The RNG to use.
         * @param maxValue The maximum value for generated integers (values will be in the range [0,
         * maxValue)).
         * @param iterations The number of values to generate and use in the standard deviation
         * calculation.
         * @return The standard deviation of the generated sample.
         */
        fun summaryStats(rng: BaseRandom,
                         maxValue: Long, iterations: Int): SynchronizedDescriptiveStatistics {
            val stats = SynchronizedDescriptiveStatistics()
            val stream = if (maxValue <= Integer.MAX_VALUE)
                rng.ints(iterations.toLong(), 0, maxValue.toInt())
            else
                rng.longs(iterations.toLong(), 0, maxValue)
            stream.spliterator().forEachRemaining { n -> stats.addValue(n.toDouble()) }
            return stats
        }

        fun <T : Random> assertEquivalentWhenSerializedAndDeserialized(rng: T) {
            val rng2 = CloneViaSerialization.clone(rng)
            assertNotSame(rng, rng2, "Deserialised RNG should be distinct object.")
            // Both RNGs should generate the same sequence.
            assertEquivalent(rng, rng2, 20, "Output mismatch after serialisation.")
            assertEquals(rng.javaClass, rng2.javaClass)
        }

        fun assertMonteCarloPiEstimateSane(rng: Random) {
            val pi = calculateMonteCarloValueForPi(rng, 100000)
            Reporter.log("Monte Carlo value for Pi: $pi")
            assertEquals(pi, Math.PI, 0.01 * Math.PI,
                    "Monte Carlo value for Pi is outside acceptable range:$pi")
        }
    }
}
