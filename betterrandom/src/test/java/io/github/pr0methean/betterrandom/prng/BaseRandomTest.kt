package io.github.pr0methean.betterrandom.prng

import io.github.pr0methean.betterrandom.TestUtils.assertGreaterOrEqual
import io.github.pr0methean.betterrandom.TestUtils.assertLessOrEqual
import io.github.pr0methean.betterrandom.prng.BaseRandom.ENTROPY_OF_DOUBLE
import io.github.pr0methean.betterrandom.prng.BaseRandom.ENTROPY_OF_FLOAT
import io.github.pr0methean.betterrandom.prng.RandomTestUtils.assertMonteCarloPiEstimateSane
import io.github.pr0methean.betterrandom.prng.RandomTestUtils.checkRangeAndEntropy
import io.github.pr0methean.betterrandom.prng.RandomTestUtils.checkStream
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.powermock.api.mockito.PowerMockito.doAnswer
import org.powermock.api.mockito.PowerMockito.`when`
import org.testng.Assert.assertEquals
import org.testng.Assert.assertFalse
import org.testng.Assert.assertNotEquals
import org.testng.Assert.assertNull
import org.testng.Assert.assertSame
import org.testng.Assert.assertTrue
import org.testng.Assert.fail

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import io.github.pr0methean.betterrandom.CloneViaSerialization
import io.github.pr0methean.betterrandom.TestUtils
import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode
import io.github.pr0methean.betterrandom.prng.adapter.SplittableRandomAdapter
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator
import io.github.pr0methean.betterrandom.seed.FakeSeedGenerator
import io.github.pr0methean.betterrandom.seed.RandomSeederThread
import io.github.pr0methean.betterrandom.seed.SeedException
import io.github.pr0methean.betterrandom.seed.SeedGenerator
import io.github.pr0methean.betterrandom.seed.SemiFakeSeedGenerator
import java.io.IOException
import java.io.Serializable
import java.lang.reflect.InvocationTargetException
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.util.Arrays
import java.util.Collections
import java.util.HashMap
import java.util.Random
import java.util.SortedSet
import java.util.TreeSet
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask
import java.util.concurrent.TimeUnit
import java.util.function.DoubleConsumer
import java.util.function.Function
import java.util.function.Supplier
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.mockpolicies.Slf4jMockPolicy
import org.powermock.core.classloader.annotations.MockPolicy
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.testng.PowerMockTestCase
import org.powermock.reflect.Whitebox
import org.testng.Reporter
import org.testng.annotations.AfterClass
import org.testng.annotations.Test

abstract class BaseRandomTest : PowerMockTestCase() {
    protected val pool = ForkJoinPool(2)

    protected val testSeedGenerator: SeedGenerator
        get() = SEMIFAKE_SEED_GENERATOR

    protected val entropyCheckMode: EntropyCheckMode
        get() = EntropyCheckMode.EXACT

    protected abstract val classUnderTest: Class<out BaseRandom>

    @Test(timeOut = 120000)
    @Throws(SeedException::class, IllegalAccessException::class, InstantiationException::class, InvocationTargetException::class)
    open fun testAllPublicConstructors() {
        TestUtils.testConstructors(classUnderTest, false, ImmutableMap.copyOf(constructorParams()),
                Consumer<out BaseRandom> { it.nextInt() })
    }

    protected open fun constructorParams(): Map<Class<*>, Any> {
        val seedLength = getNewSeedLength(createRng())
        val params = HashMap<Class<*>, Any>(4)
        params[Int::class.javaPrimitiveType] = seedLength
        params[Long::class.javaPrimitiveType] = TEST_SEED
        params[ByteArray::class.java] = ByteArray(seedLength)
        params[SeedGenerator::class.java] = SEMIFAKE_SEED_GENERATOR
        return params
    }

    protected open fun getNewSeedLength(basePrng: BaseRandom): Int {
        return basePrng.newSeedLength
    }

    /**
     * Test to ensure that two distinct RNGs with the same seed return the same sequence of numbers.
     */
    @Test(timeOut = 15000)
    @Throws(SeedException::class)
    open fun testRepeatability() {
        val rng = createRng()
        // Create second RNG using same seed.
        val duplicateRNG = createRng(rng.seed)
        RandomTestUtils.assertEquivalent(rng, duplicateRNG, 100, "Output mismatch")
    }

    /**
     * Test that nextGaussian never returns a stale cached value.
     */
    @Test(timeOut = 15000)
    @Throws(SeedException::class)
    open fun testRepeatabilityNextGaussian() {
        val rng = createRng()
        val seed = testSeedGenerator.generateSeed(getNewSeedLength(rng))
        rng.nextGaussian()
        rng.seed = seed
        // Create second RNG using same seed.
        val duplicateRNG = createRng(seed)
        assertEquals(rng.nextGaussian(), duplicateRNG.nextGaussian())
    }

    @Test(timeOut = 15000, expectedExceptions = arrayOf(IllegalArgumentException::class))
    @Throws(GeneralSecurityException::class, SeedException::class)
    open fun testSeedTooLong() {
        createRng(testSeedGenerator
                .generateSeed(getNewSeedLength(createRng()) + 1)) // Should throw an exception.
    }

    protected abstract fun createRng(): BaseRandom

    protected abstract fun createRng(seed: ByteArray?): BaseRandom

    /**
     * Test to ensure that the output from the RNG is broadly as expected.  This will not detect the
     * subtle statistical anomalies that would be picked up by Diehard, but it provides a simple check
     * for major problems with the output.
     */
    @Test(timeOut = 20000, groups = arrayOf("non-deterministic"))
    @Throws(SeedException::class)
    open fun testDistribution() {
        val rng = createRng()
        assertMonteCarloPiEstimateSane(rng)
    }

    /**
     * Test to ensure that the output from the RNG is broadly as expected.  This will not detect the
     * subtle statistical anomalies that would be picked up by Diehard, but it provides a simple check
     * for major problems with the output.
     */
    @Test(timeOut = 30000, groups = arrayOf("non-deterministic"))
    @Throws(SeedException::class)
    open fun testIntegerSummaryStats() {
        val rng = createRng()
        // Expected standard deviation for a uniformly distributed population of values in the range 0..n
        // approaches n/sqrt(12).
        // Expected standard deviation for a uniformly distributed population of values in the range 0..n
        // approaches n/sqrt(12).
        for (n in longArrayOf(100, 1L shl 32, java.lang.Long.MAX_VALUE)) {
            val iterations = 10000
            val stats = RandomTestUtils.summaryStats(rng, n, iterations)
            val observedSD = stats.standardDeviation
            val expectedSD = n / SQRT_12
            Reporter.log("Expected SD: $expectedSD, observed SD: $observedSD")
            assertGreaterOrEqual(observedSD, 0.97 * expectedSD)
            assertLessOrEqual(observedSD, 1.03 * expectedSD)
            assertGreaterOrEqual(stats.max, 0.9 * n)
            assertLessOrEqual(stats.max, (n - 1).toDouble())
            assertGreaterOrEqual(stats.min, 0.0)
            assertLessOrEqual(stats.min, 0.1 * n)
            assertGreaterOrEqual(stats.mean, 0.4 * n)
            assertLessOrEqual(stats.mean, 0.6 * n)
            val median = stats.getPercentile(50.0)
            assertGreaterOrEqual(median, 0.4 * n)
            assertLessOrEqual(median, 0.6 * n)
        }
    }

    /**
     * Test to ensure that the output from nextGaussian is broadly as expected.
     */
    @Test(timeOut = 40000, groups = arrayOf("non-deterministic"))
    @Throws(SeedException::class)
    fun testNextGaussianStatistically() {
        val rng = createRng()
        val iterations = 20000
        val stats = SynchronizedDescriptiveStatistics()
        rng.gaussians(iterations.toLong()).spliterator().forEachRemaining(DoubleConsumer { stats.addValue(it) })
        val observedSD = stats.standardDeviation
        Reporter.log("Expected SD for Gaussians: 1, observed SD: $observedSD")
        assertGreaterOrEqual(observedSD, 0.965)
        assertLessOrEqual(observedSD, 1.035)
        assertGreaterOrEqual(stats.max, 2.0)
        assertLessOrEqual(stats.min, -2.0)
        assertGreaterOrEqual(stats.mean, -0.1)
        assertLessOrEqual(stats.mean, 0.1)
        val median = stats.getPercentile(50.0)
        assertGreaterOrEqual(median, -0.1)
        assertLessOrEqual(median, 0.1)
    }

    /**
     * Make sure that the RNG does not accept seeds that are too small since this could affect the
     * distribution of the output.
     */
    @Test(timeOut = 15000, expectedExceptions = arrayOf(IllegalArgumentException::class))
    @Throws(SeedException::class)
    open fun testSeedTooShort() {
        createRng(byteArrayOf(1, 2, 3)) // One byte too few, should cause an IllegalArgumentException.
    }

    /**
     * RNG must not accept a null seed otherwise it will not be properly initialised.
     */
    @Test(timeOut = 15000, expectedExceptions = arrayOf(IllegalArgumentException::class))
    @Throws(SeedException::class)
    open fun testNullSeed() {
        createRng(null)
    }

    @Test(timeOut = 15000)
    @Throws(IOException::class, ClassNotFoundException::class, SeedException::class)
    open fun testSerializable() {
        // Serialise an RNG.
        val rng = createRng()
        RandomTestUtils.assertEquivalentWhenSerializedAndDeserialized(rng)
        // Can't use a SemiFakeSeedGenerator, because Random.equals() breaks equality check
        val seedGenerator = FakeSeedGenerator(
                javaClass.simpleName + "::testSerializable")
        rng.setSeedGenerator(seedGenerator)
        try {
            val rng2 = CloneViaSerialization.clone(rng)
            try {
                assertEquals(seedGenerator, rng2.getSeedGenerator())
            } finally {
                rng2.setSeedGenerator(null)
            }
        } finally {
            rng.setSeedGenerator(null)
            RandomSeederThread.stopIfEmpty(seedGenerator)
        }
    }

    /** Assertion-free since many implementations have a fallback behavior.  */
    @Test(timeOut = 60000)
    open fun testSetSeedLong() {
        createRng().setSeed(0x0123456789ABCDEFL)
    }

    @Test(timeOut = 15000)
    @Throws(SeedException::class)
    open fun testSetSeedAfterNextLong() {
        val seed = testSeedGenerator.generateSeed(getNewSeedLength(createRng()))
        val rng = createRng()
        val rng2 = createRng()
        val rng3 = createRng(seed)
        rng.nextLong() // ensure rng & rng2 won't both be in initial state before reseeding
        rng.seed = seed
        rng2.seed = seed
        RandomTestUtils.assertEquivalent(rng, rng2, 64,
                "Output mismatch after reseeding with same seed")
        rng.seed = seed
        RandomTestUtils.assertEquivalent(rng, rng3, 64,
                "Output mismatch vs a new PRNG with same seed")
    }

    @Test(timeOut = 15000)
    @Throws(SeedException::class)
    open fun testSetSeedAfterNextInt() {
        val seed = testSeedGenerator.generateSeed(getNewSeedLength(createRng()))
        val rng = createRng()
        val rng2 = createRng()
        val rng3 = createRng(seed)
        rng.nextInt() // ensure rng & rng2 won't both be in initial state before reseeding
        rng.seed = seed
        rng2.seed = seed
        RandomTestUtils.assertEquivalent(rng, rng2, 64,
                "Output mismatch after reseeding with same seed")
        rng.seed = seed
        RandomTestUtils.assertEquivalent(rng, rng3, 64,
                "Output mismatch vs a new PRNG with same seed")
    }

    @Test(timeOut = 15000)
    @Throws(SeedException::class)
    open fun testSetSeedZero() {
        val length = getNewSeedLength(createRng())
        val zeroSeed = ByteArray(length)
        val realSeed = ByteArray(length)
        do {
            testSeedGenerator.generateSeed(realSeed)
        } while (Arrays.equals(realSeed, zeroSeed))
        val rng = createRng(realSeed)
        val rng2 = createRng(zeroSeed)
        RandomTestUtils.assertDistinct(rng, rng2, 20,
                "Output with real seed matches output with all-zeroes seed")
    }

    @Test(timeOut = 15000)
    @Throws(SeedException::class)
    open fun testEquals() {
        RandomTestUtils.doEqualsSanityChecks { this.createRng() }
    }

    @Test(timeOut = 60000)
    @Throws(Exception::class)
    open fun testHashCode() {
        assert(RandomTestUtils.testHashCodeDistribution { this.createRng() }) { "Too many hashCode collisions" }
    }

    /**
     * dump() doesn't have much of a contract, but we should at least expect it to output enough state
     * for two independently-generated instances to give unequal dumps.
     */
    @Test(timeOut = 15000)
    @Throws(SeedException::class)
    open fun testDump() {
        val rng = createRng()
        assertNotEquals(rng.dump(), createRng().dump())
        rng.nextLong() // Kill a mutant where dump doesn't unlock the lock
    }

    @Test
    @Throws(SeedException::class)
    open fun testReseeding() {
        val output1 = ByteArray(20)
        val rng1 = createRng()
        val rng2 = createRng()
        rng1.nextBytes(output1)
        val output2 = ByteArray(20)
        rng2.nextBytes(output2)
        val seedLength = rng1.newSeedLength
        rng1.seed = testSeedGenerator.generateSeed(seedLength)
        assertGreaterOrEqual(rng1.entropyBits, seedLength * 8L)
        rng1.nextBytes(output1)
        rng2.nextBytes(output2)
        assertFalse(Arrays.equals(output1, output2))
    }

    /**
     * This also tests [BaseRandom.getSeedGenerator] and
     * [BaseRandom.setSeedGenerator].
     *
     * @throws Exception
     */
    @Test(timeOut = 60000)
    @Throws(Exception::class)
    open fun testRandomSeederThreadIntegration() {
        val seedGenerator = SemiFakeSeedGenerator(Random())
        val rng = createRng()
        val oldSeed = rng.seed
        while (rng.entropyBits > java.lang.Long.SIZE) {
            rng.nextLong()
        }
        RandomSeederThread.setPriority(seedGenerator, Thread.MAX_PRIORITY)
        rng.setSeedGenerator(seedGenerator)
        try {
            var waits = 0
            var newSeed: ByteArray
            do {
                assertSame(rng.getSeedGenerator(), seedGenerator)
                rng.nextBoolean()
                Thread.sleep(10)
                waits++
                newSeed = rng.seed
            } while (Arrays.equals(newSeed, oldSeed) && waits < 1000)
            if (waits >= 1000) {
                fail(String.format("Timed out waiting for %s to be reseeded!", rng))
            }
            Thread.sleep(100) // entropy update may not be co-atomic with seed update
            assertGreaterOrEqual(rng.entropyBits, newSeed.size * 8L - 1)
        } finally {
            rng.setSeedGenerator(null)
            RandomSeederThread.stopIfEmpty(seedGenerator)
        }
        assertNull(rng.getSeedGenerator())
    }

    @Test(timeOut = 10000)
    fun testWithProbability() {
        val prng = createRng()
        val originalEntropy = prng.entropyBits
        assertFalse(prng.withProbability(0.0))
        assertTrue(prng.withProbability(1.0))
        assertEquals(originalEntropy, prng.entropyBits)
        checkRangeAndEntropy(prng, 1, { if (prng.withProbability(0.7)) 0 else 1 }, 0.0, 2.0,
                entropyCheckMode)
    }

    @Test(timeOut = 20000, groups = arrayOf("non-deterministic"))
    fun testWithProbabilityStatistically() {
        val prng = createRng()
        var trues = 0
        for (i in 0..2999) {
            if (prng.withProbability(0.6)) {
                trues++
            }
        }
        // Significance test at p=3.15E-6 (unadjusted for the multiple subclasses and environments!)
        assertGreaterOrEqual(trues.toLong(), 1675)
        assertLessOrEqual(trues.toLong(), 1925)
        trues = 0
        for (i in 0..2999) {
            if (prng.withProbability(0.5)) {
                trues++
            }
        }
        // Significance test at p=4.54E-6 (unadjusted for the multiple subclasses and environments!)
        assertGreaterOrEqual(trues.toLong(), 1375)
        assertLessOrEqual(trues.toLong(), 1625)
    }

    @Test(timeOut = 20000, groups = arrayOf("non-deterministic"))
    fun testNextBooleanStatistically() {
        val prng = createRng()
        var trues = 0
        for (i in 0..2999) {
            if (prng.nextBoolean()) {
                trues++
            }
        }
        // Significance test at p=4.54E-6 (unadjusted for the multiple subclasses and environments!)
        assertGreaterOrEqual(trues.toLong(), 1375)
        assertLessOrEqual(trues.toLong(), 1625)
    }

    @Test
    @Throws(Exception::class)
    fun testNextBytes() {
        val testBytes = ByteArray(TEST_BYTE_ARRAY_LENGTH)
        val prng = createRng()
        val oldEntropy = prng.entropyBits
        prng.nextBytes(testBytes)
        assertFalse(Arrays.equals(testBytes, ByteArray(TEST_BYTE_ARRAY_LENGTH)))
        val entropy = prng.entropyBits
        val expectedEntropy = oldEntropy - 8 * TEST_BYTE_ARRAY_LENGTH
        when (entropyCheckMode) {
            RandomTestUtils.EntropyCheckMode.EXACT -> assertEquals(entropy, expectedEntropy)
            RandomTestUtils.EntropyCheckMode.LOWER_BOUND -> assertGreaterOrEqual(entropy, expectedEntropy)
            RandomTestUtils.EntropyCheckMode.OFF -> {
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testNextInt1() {
        val prng = createRng()
        val numberSupplier = { prng.nextInt(3 shl 29) }
        checkRangeAndEntropy(prng, 31, numberSupplier, 0.0, (3 shl 29).toDouble(), entropyCheckMode)
    }

    @Test(expectedExceptions = arrayOf(IllegalArgumentException::class))
    fun testNextInt1InvalidBound() {
        createRng().nextInt(0)
    }

    @Test
    @Throws(Exception::class)
    fun testNextInt() {
        val prng = createRng()
        checkRangeAndEntropy(prng, 32, Supplier<Number> { prng.nextInt() } as Supplier<out Number>, Integer.MIN_VALUE.toDouble(),
                (Integer.MAX_VALUE + 1L).toDouble(), entropyCheckMode)
    }

    @Test
    @Throws(Exception::class)
    fun testNextInt2() {
        val prng = createRng()
        val numberSupplier = { prng.nextInt(1 shl 27, 1 shl 29) }
        checkRangeAndEntropy(prng, 29, numberSupplier, (1 shl 27).toDouble(), (1 shl 29).toDouble(), entropyCheckMode)
    }

    @Test(expectedExceptions = arrayOf(IllegalArgumentException::class))
    fun testNextInt2InvalidBound() {
        createRng().nextInt(1, 1)
    }

    @Test
    @Throws(Exception::class)
    fun testNextInt2HugeRange() {
        val prng = createRng()
        val numberSupplier = { prng.nextInt(Integer.MIN_VALUE, 1 shl 29) }
        checkRangeAndEntropy(prng, 32, numberSupplier, Integer.MIN_VALUE.toDouble(), (1 shl 29).toDouble(),
                entropyCheckMode)
    }

    @Test
    @Throws(Exception::class)
    fun testNextLong() {
        val prng = createRng()
        checkRangeAndEntropy(prng, 64, Supplier<Number> { prng.nextLong() } as Supplier<out Number>, java.lang.Long.MIN_VALUE.toDouble(),
                java.lang.Long.MAX_VALUE + 1.0, entropyCheckMode)
    }

    @Test
    @Throws(Exception::class)
    fun testNextLong1() {
        val prng = createRng()
        for (i in 0..19) {
            // check that the bound is exclusive, to kill an off-by-one mutant
            val numberSupplier = { prng.nextLong(2) }
            checkRangeAndEntropy(prng, 1, numberSupplier, 0.0, 2.0, entropyCheckMode)
        }
        val numberSupplier = { prng.nextLong(1L shl 42) }
        checkRangeAndEntropy(prng, 42, numberSupplier, 0.0, (1L shl 42).toDouble(), entropyCheckMode)
    }

    @Test(expectedExceptions = arrayOf(IllegalArgumentException::class))
    fun testNextLong1InvalidBound() {
        createRng().nextLong(-1)
    }

    @Test
    @Throws(Exception::class)
    fun testNextLong2() {
        val prng = createRng()
        val numberSupplier = { prng.nextLong(1L shl 40, 1L shl 42) }
        checkRangeAndEntropy(prng, 42, numberSupplier, (1L shl 40).toDouble(), (1L shl 42).toDouble(), entropyCheckMode)
    }

    @Test(expectedExceptions = arrayOf(IllegalArgumentException::class))
    fun testNextLong2InvalidBound() {
        createRng().nextLong(10, 9)
    }

    @Test
    @Throws(Exception::class)
    fun testNextLong2HugeRange() {
        val prng = createRng()
        val numberSupplier = { prng.nextLong(java.lang.Long.MIN_VALUE, 1L shl 62) }
        checkRangeAndEntropy(prng, 64, numberSupplier, java.lang.Long.MIN_VALUE.toDouble(), (1L shl 62).toDouble(),
                entropyCheckMode)
    }

    @Test
    @Throws(Exception::class)
    fun testNextDouble() {
        val prng = createRng()
        checkRangeAndEntropy(prng, ENTROPY_OF_DOUBLE.toLong(), Supplier<Number> { prng.nextDouble() } as Supplier<out Number>,
                0.0, 1.0, entropyCheckMode)
    }

    @Test
    @Throws(Exception::class)
    fun testNextFloat() {
        val prng = createRng()
        checkRangeAndEntropy(prng, ENTROPY_OF_FLOAT.toLong(), Supplier<Number> { prng.nextFloat() } as Supplier<out Number>, 0.0,
                1.0, entropyCheckMode)
    }

    @Test
    @Throws(Exception::class)
    fun testNextDouble1() {
        val prng = createRng()
        val numberSupplier = { prng.nextDouble(13.37) }
        checkRangeAndEntropy(prng, ENTROPY_OF_DOUBLE.toLong(), numberSupplier, 0.0, 13.37,
                entropyCheckMode)
    }

    @Test(expectedExceptions = arrayOf(IllegalArgumentException::class))
    fun testNextDouble1InvalidBound() {
        createRng().nextDouble(-1.0)
    }

    @Test
    @Throws(Exception::class)
    fun testNextDouble2() {
        val prng = createRng()
        val numberSupplier2 = { prng.nextDouble(-1.0, 13.37) }
        checkRangeAndEntropy(prng, ENTROPY_OF_DOUBLE.toLong(), numberSupplier2, -1.0, 13.37,
                entropyCheckMode)
        val numberSupplier1 = { prng.nextDouble(5.0, 13.37) }
        checkRangeAndEntropy(prng, ENTROPY_OF_DOUBLE.toLong(), numberSupplier1, 5.0, 13.37,
                entropyCheckMode)
        val numberSupplier = { prng.nextDouble(1.0, UPPER_BOUND_FOR_ROUNDING_TEST) }
        checkRangeAndEntropy(prng, ENTROPY_OF_DOUBLE.toLong(), numberSupplier, 1.0,
                UPPER_BOUND_FOR_ROUNDING_TEST, entropyCheckMode)
    }

    @Test(expectedExceptions = arrayOf(IllegalArgumentException::class))
    fun testNextDouble2InvalidBound() {
        createRng().nextDouble(3.5, 3.5)
    }

    @Test
    @Throws(Exception::class)
    fun testNextGaussian() {
        val prng = createRng()
        // TODO: Find out the actual Shannon entropy of nextGaussian() and adjust the entropy count to
        // it in a wrapper function.
        checkRangeAndEntropy(prng, (2 * ENTROPY_OF_DOUBLE).toLong(),
                { prng.nextGaussian() + prng.nextGaussian() }, -java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE,
                entropyCheckMode)
    }

    @Test
    @Throws(Exception::class)
    fun testNextBoolean() {
        val prng = createRng()
        val numberSupplier = { if (prng.nextBoolean()) 0 else 1 }
        checkRangeAndEntropy(prng, 1L, numberSupplier, 0.0, 2.0, entropyCheckMode)
    }

    @Test
    @Throws(Exception::class)
    fun testInts() {
        val prng = createRng()
        checkStream(prng, 32, prng.ints().boxed(), -1, Integer.MIN_VALUE.toDouble(), (Integer.MAX_VALUE + 1L).toDouble(), true)
    }

    @Test
    @Throws(Exception::class)
    fun testInts1() {
        val prng = createRng()
        checkStream(prng, 32, prng.ints(20).boxed(), 20, Integer.MIN_VALUE.toDouble(), (Integer.MAX_VALUE + 1L).toDouble(),
                true)
    }

    @Test
    @Throws(Exception::class)
    fun testInts2() {
        val prng = createRng()
        checkStream(prng, 29, prng.ints(1 shl 27, 1 shl 29).boxed(), -1, (1 shl 27).toDouble(), (1 shl 29).toDouble(), true)
    }

    @Test
    @Throws(Exception::class)
    fun testInts3() {
        val prng = createRng()
        checkStream(prng, 29, prng.ints(3, 1 shl 27, 1 shl 29).boxed(), 3, (1 shl 27).toDouble(), (1 shl 29).toDouble(), true)
    }

    @Test
    @Throws(Exception::class)
    fun testLongs() {
        val prng = createRng()
        checkStream(prng, 64, prng.longs().boxed(), -1, java.lang.Long.MIN_VALUE.toDouble(), java.lang.Long.MAX_VALUE + 1.0, true)
    }

    @Test
    @Throws(Exception::class)
    fun testLongs1() {
        val prng = createRng()
        checkStream(prng, 64, prng.longs(20).boxed(), 20, java.lang.Long.MIN_VALUE.toDouble(), java.lang.Long.MAX_VALUE + 1.0, true)
    }

    @Test
    @Throws(Exception::class)
    fun testLongs2() {
        val prng = createRng()
        checkStream(prng, 42, prng.longs(1L shl 40, 1L shl 42).boxed(), -1, (1L shl 40).toDouble(), (1L shl 42).toDouble(), true)
    }

    @Test
    @Throws(Exception::class)
    fun testLongs3() {
        val prng = createRng()
        checkStream(prng, 42, prng.longs(20, 1L shl 40, 1L shl 42).boxed(), 20, (1L shl 40).toDouble(), (1L shl 42).toDouble(), true)
    }

    @Test(timeOut = 10000)
    @Throws(Exception::class)
    fun testLongs3SmallRange() {
        val bound = (1L shl 40) + 2
        val prng = createRng()
        checkStream(prng, 31, prng.longs(20, 1L shl 40, bound).boxed(), 20, (1L shl 40).toDouble(), bound.toDouble(), true)
    }

    @Test
    @Throws(Exception::class)
    fun testDoubles() {
        val prng = createRng()
        checkStream(prng, ENTROPY_OF_DOUBLE.toLong(), prng.doubles().boxed(), -1, 0.0, 1.0, true)
    }

    @Test
    @Throws(Exception::class)
    fun testDoubles1() {
        val prng = createRng()
        checkStream(prng, ENTROPY_OF_DOUBLE.toLong(), prng.doubles(20).boxed(), 20, 0.0, 1.0, true)
    }

    @Test
    @Throws(Exception::class)
    fun testDoubles2() {
        val prng = createRng()
        checkStream(prng, ENTROPY_OF_DOUBLE.toLong(), prng.doubles(-5.0, 8.0).boxed(), -1, -5.0, 8.0, true)
    }

    @Test
    @Throws(Exception::class)
    fun testDoubles3() {
        val prng = createRng()
        checkStream(prng, ENTROPY_OF_DOUBLE.toLong(), prng.doubles(20, -5.0, 8.0).boxed(), 20, -5.0, 8.0, true)
    }

    @Test
    @Throws(Exception::class)
    fun testDoubles3RoundingCorrection() {
        val prng = createRng()
        checkStream(prng, ENTROPY_OF_DOUBLE.toLong(),
                prng.doubles(20, 1.0, UPPER_BOUND_FOR_ROUNDING_TEST).boxed(), 20, -5.0, 8.0, true)
    }

    @Test
    @Throws(Exception::class)
    fun testGaussians() {
        val prng = createRng()
        checkStream(prng, ENTROPY_OF_DOUBLE.toLong(), prng.gaussians().boxed(), -1, -java.lang.Double.MAX_VALUE,
                java.lang.Double.MAX_VALUE, true)
    }

    @Test
    @Throws(Exception::class)
    fun testGaussians1() {
        val prng = createRng()
        checkStream(prng, ENTROPY_OF_DOUBLE.toLong(), prng.gaussians(100).boxed(), 100, -java.lang.Double.MAX_VALUE,
                java.lang.Double.MAX_VALUE, true)
    }

    @Test
    fun testNextElementArray() {
        val prng = createRng()
        testGeneratesAll({ prng.nextElement(STRING_ARRAY) }, *STRING_ARRAY)
    }

    @Test
    fun testNextElementList() {
        val prng = createRng()
        testGeneratesAll({ prng.nextElement(STRING_LIST) }, *STRING_ARRAY)
    }

    @Test
    fun testNextEnum() {
        val prng = createRng()
        testGeneratesAll({ prng.nextEnum(TestEnum::class.java) }, TestEnum.RED, TestEnum.YELLOW,
                TestEnum.BLUE)
    }

    @Test
    fun testGetNewSeedLength() {
        assertTrue(createRng().newSeedLength > 0)
    }

    @Test(timeOut = 90000)
    open fun testThreadSafety() {
        testThreadSafety(FUNCTIONS_FOR_THREAD_SAFETY_TEST, FUNCTIONS_FOR_THREAD_SAFETY_TEST)
    }

    @Test(timeOut = 90000)
    open fun testThreadSafetySetSeed() {
        testThreadSafetyVsCrashesOnly(30, FUNCTIONS_FOR_THREAD_CRASH_TEST)
    }

    protected fun testThreadSafetyVsCrashesOnly(timeoutSec: Int,
                                                functions: List<NamedFunction<Random, Double>>) {
        val seedLength = createRng().newSeedLength
        val seed = testSeedGenerator.generateSeed(seedLength)
        for (supplier1 in functions) {
            for (supplier2 in functions) {
                runParallel(supplier1, supplier2, seed, timeoutSec,
                        if (supplier1 == SET_SEED || supplier2 == SET_SEED) 200 else 1000)
            }
        }
    }

    protected fun testThreadSafety(functions: List<NamedFunction<Random, Double>>,
                                   pairwiseFunctions: List<NamedFunction<Random, Double>>) {
        val seedLength = createRng().newSeedLength
        val seed = testSeedGenerator.generateSeed(seedLength)
        for (supplier in functions) {
            for (i in 0..4) {
                // This loop is necessary to control the false pass rate, especially during mutation testing.
                val sequentialOutput = runSequential(supplier, supplier, seed)
                val parallelOutput = runParallel(supplier, supplier, seed, 10, 1000)
                assertEquals(sequentialOutput, parallelOutput,
                        "output differs between sequential & parallel calls to $supplier")
            }
        }

        // Check that each pair won't crash no matter which order they start in
        // (this part is assertion-free because we can't tell whether A-bits-as-long and
        // B-bits-as-double come from the same bit stream as vice-versa).
        for (supplier1 in pairwiseFunctions) {
            for (supplier2 in pairwiseFunctions) {
                if (supplier1 != supplier2) {
                    runParallel(supplier1, supplier2, seed, 10, 1000)
                }
            }
        }
    }

    protected fun runParallel(supplier1: NamedFunction<Random, Double>,
                              supplier2: NamedFunction<Random, Double>, seed: ByteArray, timeoutSec: Int,
                              iterations: Int): SortedSet<Double> {
        // See https://www.yegor256.com/2018/03/27/how-to-test-thread-safety.html for why a
        // CountDownLatch is used.
        val latch = CountDownLatch(2)
        val parallelPrng = createRng(seed)
        val output = ConcurrentSkipListSet<Double>()
        pool.execute(GeneratorForkJoinTask(parallelPrng, output, supplier1, latch, iterations))
        pool.execute(GeneratorForkJoinTask(parallelPrng, output, supplier2, latch, iterations))
        assertTrue(pool.awaitQuiescence(timeoutSec.toLong(), TimeUnit.SECONDS),
                String.format("Timed out waiting for %s and %s to finish", supplier1, supplier2))
        return output
    }

    protected fun runSequential(supplier1: NamedFunction<Random, Double>,
                                supplier2: NamedFunction<Random, Double>, seed: ByteArray): SortedSet<Double> {
        val sequentialPrng = createRng(seed)
        val output = TreeSet<Double>()
        GeneratorForkJoinTask(sequentialPrng, output, supplier1, CountDownLatch(1),
                1000)
                .exec()
        GeneratorForkJoinTask(sequentialPrng, output, supplier2, CountDownLatch(1),
                1000)
                .exec()
        return output
    }

    @AfterClass
    fun classTearDown() {
        RandomSeederThread.stopIfEmpty(testSeedGenerator)
    }

    private enum class TestEnum {
        RED,
        YELLOW,
        BLUE
    }

    /**
     * ForkJoinTask that reads random longs and adds them to the set.
     */
    protected class GeneratorForkJoinTask<T>(private val prng: Random, private val set: SortedSet<T>,
                                             private val function: NamedFunction<Random, T>, private val latch: CountDownLatch, private val iterations: Int) : ForkJoinTask<Void>() {

        override fun getRawResult(): Void? {
            return null
        }

        override fun setRawResult(value: Void) {
            // No-op.
        }

        public override fun exec(): Boolean {
            latch.countDown()
            try {
                latch.await()
            } catch (e: InterruptedException) {
                throw AssertionError("Interrupted", e)
            }

            for (i in 0 until iterations) {
                set.add(function.apply(prng))
            }
            return true
        }

        companion object {

            private val serialVersionUID = 9155874155769888368L
        }
    }

    protected class NamedFunction<T, R>(private val function: Function<T, R>, private val name: String) : Function<T, R>, Serializable {

        override fun apply(t: T): R {
            return function.apply(t)
        }

        override fun toString(): String {
            return name
        }
    }

    companion object {

        protected val SEMIFAKE_SEED_GENERATOR: SeedGenerator = SemiFakeSeedGenerator(SplittableRandomAdapter())

        /**
         * The square root of 12, rounded from an extended-precision calculation that was done by Wolfram
         * Alpha (and thus at least as accurate as `StrictMath.sqrt(12.0)`).
         */
        protected val SQRT_12 = 3.4641016151377546
        protected val TEST_SEED = 0x0123456789ABCDEFL
        protected val NEXT_LONG = NamedFunction<Random, Double>({ random -> random.nextLong().toDouble() }, "Random::nextLong")
        protected val NEXT_INT = NamedFunction<Random, Double>({ random -> random.nextInt().toDouble() }, "Random::nextInt")
        protected val NEXT_DOUBLE = NamedFunction(Function<Random, Double> { it.nextDouble() }, "Random::nextDouble")
        protected val NEXT_GAUSSIAN = NamedFunction(Function<Random, Double> { it.nextGaussian() }, "Random::nextGaussian")
        protected val SET_SEED = NamedFunction<Random, Double>({ random ->
            if (random is BaseRandom) {
                val baseRandom = random as BaseRandom
                baseRandom.seed = SEMIFAKE_SEED_GENERATOR.generateSeed(baseRandom.newSeedLength)
            } else {
                val buffer = ByteBuffer.allocate(8)
                SEMIFAKE_SEED_GENERATOR.generateSeed(buffer.array())
                random.setSeed(buffer.getLong(0))
            }
            0.0
        }, "BaseRandom::setSeed(byte[])")

        protected val FUNCTIONS_FOR_THREAD_SAFETY_TEST: List<NamedFunction<Random, Double>> = ImmutableList.of(NEXT_LONG, NEXT_INT, NEXT_DOUBLE, NEXT_GAUSSIAN)
        protected val FUNCTIONS_FOR_THREAD_CRASH_TEST: List<NamedFunction<Random, Double>> = ImmutableList.of(NEXT_LONG, NEXT_INT, NEXT_DOUBLE, NEXT_GAUSSIAN, SET_SEED)
        private val TEST_BYTE_ARRAY_LENGTH = 20
        private val HELLO = "Hello"
        private val HOW_ARE_YOU = "How are you?"
        private val GOODBYE = "Goodbye"
        private val STRING_ARRAY = arrayOf(HELLO, HOW_ARE_YOU, GOODBYE)
        private val STRING_LIST = Collections.unmodifiableList(Arrays.asList(*STRING_ARRAY))
        private val ELEMENTS = 100
        private val UPPER_BOUND_FOR_ROUNDING_TEST = java.lang.Double.longBitsToDouble(java.lang.Double.doubleToLongBits(1.0) + 3)

        @SafeVarargs
        private fun <E> testGeneratesAll(generator: Supplier<E>, vararg expected: E) {
            val selected = Arrays.copyOf(expected, ELEMENTS) // Saves passing in a Class<E>
            for (i in 0 until ELEMENTS) {
                selected[i] = generator.get()
            }
            assertTrue(Arrays.asList(*selected).containsAll(Arrays.asList(*expected)))
        }
    }
}
