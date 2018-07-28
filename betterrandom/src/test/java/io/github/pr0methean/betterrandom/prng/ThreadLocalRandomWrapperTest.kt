package io.github.pr0methean.betterrandom.prng

import org.testng.Assert.assertEquals

import io.github.pr0methean.betterrandom.CloneViaSerialization
import io.github.pr0methean.betterrandom.seed.SeedException
import io.github.pr0methean.betterrandom.seed.SeedGenerator
import java.io.Serializable
import java.util.Random
import java.util.function.Function
import java.util.function.Supplier
import org.testng.annotations.Test

@Test(testName = "ThreadLocalRandomWrapper")
open class ThreadLocalRandomWrapperTest : BaseRandomTest() {

    private val pcgSupplier: Supplier<BaseRandom>

    protected override val classUnderTest: Class<out BaseRandom>
        get() = ThreadLocalRandomWrapper::class.java

    init {
        // Must be done first, or else lambda won't be serializable.
        val seedGenerator = testSeedGenerator

        pcgSupplier = { Pcg64Random(seedGenerator) } as Supplier<BaseRandom>
    }

    @Throws(SeedException::class)
    override fun testSerializable() {
        // May change after serialization, so test only that it still works at all afterward
        CloneViaSerialization.clone(createRng()).nextInt()
    }

    @Test(timeOut = 15000, expectedExceptions = arrayOf(IllegalArgumentException::class))
    @Throws(SeedException::class)
    override fun testSeedTooLong() {
        createRng().setSeed(testSeedGenerator.generateSeed(17))
    }

    @Test(timeOut = 15000, expectedExceptions = arrayOf(IllegalArgumentException::class))
    @Throws(SeedException::class)
    override fun testSeedTooShort() {
        createRng().setSeed(byteArrayOf(1, 2, 3))
    }

    @Test(timeOut = 15000, expectedExceptions = arrayOf(IllegalArgumentException::class))
    @Throws(SeedException::class)
    override fun testNullSeed() {
        createRng().setSeed(null)
    }

    @Test(enabled = false)
    @Throws(SeedException::class)
    override fun testRepeatability() {
        // No-op: ThreadLocalRandomWrapper isn't repeatable.
    }

    @Test(enabled = false)
    override fun testRepeatabilityNextGaussian() {
        // No-op: ThreadLocalRandomWrapper isn't repeatable.
    }

    /** Seeding of this PRNG is thread-local, so setSeederThread makes no sense.  */
    @Test(expectedExceptions = arrayOf(UnsupportedOperationException::class))
    override fun testRandomSeederThreadIntegration() {
        createRng().setSeedGenerator(testSeedGenerator)
    }

    /** Assertion-free because ThreadLocalRandomWrapper isn't repeatable.  */
    @Test
    @Throws(SeedException::class)
    override fun testSetSeedAfterNextLong() {
        val seed = testSeedGenerator.generateSeed(getNewSeedLength(createRng()))
        val rng = createRng()
        rng.nextLong()
        rng.setSeed(seed)
    }

    /** Assertion-free because ThreadLocalRandomWrapper isn't repeatable.  */
    @Test
    @Throws(SeedException::class)
    override fun testSetSeedAfterNextInt() {
        val seed = testSeedGenerator.generateSeed(getNewSeedLength(createRng()))
        val rng = createRng()
        rng.nextInt()
        rng.setSeed(seed)
    }


    /** Assertion-free because thread-local.  */
    @Test
    override fun testThreadSafety() {
        testThreadSafetyVsCrashesOnly(30, BaseRandomTest.FUNCTIONS_FOR_THREAD_SAFETY_TEST)
    }

    public override fun constructorParams(): Map<Class<*>, Any> {
        val params = super.constructorParams()
        params[Supplier<*>::class.java] = pcgSupplier
        params[Function<*, *>::class.java] = Function<ByteArray, BaseRandom> { Pcg64Random(it) }
        return params
    }

    @Test
    @Throws(SeedException::class)
    fun testExplicitSeedSize() {
        assertEquals(ThreadLocalRandomWrapper(200, testSeedGenerator,
                Function { AesCounterRandom(it) }).newSeedLength, 200)
    }

    @Test
    @Throws(SeedException::class)
    open fun testWrapLegacy() {
        ThreadLocalRandomWrapper.wrapLegacy(LongFunction<Random> { Random(it) }, testSeedGenerator).nextInt()
    }

    @Throws(SeedException::class)
    override fun createRng(): BaseRandom {
        return ThreadLocalRandomWrapper(pcgSupplier)
    }

    @Throws(SeedException::class)
    override fun createRng(seed: ByteArray?): BaseRandom {
        val rng = createRng()
        rng.setSeed(seed)
        return rng
    }
}
