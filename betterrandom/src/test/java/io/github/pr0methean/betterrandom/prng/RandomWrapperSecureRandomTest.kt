package io.github.pr0methean.betterrandom.prng

import com.google.common.collect.ImmutableList
import io.github.pr0methean.betterrandom.seed.SeedException
import java.security.GeneralSecurityException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.Random
import org.testng.annotations.Test

@Test(testName = "RandomWrapper:SecureRandom")
class RandomWrapperSecureRandomTest : BaseRandomTest() {

    protected override val classUnderTest: Class<out BaseRandom>
        get() = RandomWrapper::class.java

    public override fun constructorParams(): Map<Class<*>, Any> {
        val params = super.constructorParams()
        params[Random::class.java] = SecureRandom()
        return params
    }

    /**
     * [SecureRandom.setSeed] has no length restriction, so disinherit [ ][Test.expectedExceptions].
     */
    @Test
    @Throws(GeneralSecurityException::class, SeedException::class)
    override fun testSeedTooLong() {
        super.testSeedTooLong()
    }

    /**
     * [SecureRandom.setSeed] has no length restriction, so disinherit [ ][Test.expectedExceptions].
     */
    @Test
    @Throws(SeedException::class)
    override fun testSeedTooShort() {
        super.testSeedTooShort()
    }

    @Test(enabled = false)
    @Throws(SeedException::class)
    override fun testNullSeed() {
        // No-op.
    }

    /**
     * Only test for crashes, since [SecureRandom.setSeed] doesn't completely replace the
     * existing seed.
     */
    @Throws(SeedException::class)
    override fun testSetSeedAfterNextLong() {
        val prng = createRng()
        prng.nextLong()
        prng.seed = testSeedGenerator.generateSeed(8)
        prng.nextLong()
    }

    /**
     * Only test for crashes, since [SecureRandom.setSeed] doesn't completely replace the
     * existing seed.
     */
    @Throws(SeedException::class)
    override fun testSetSeedAfterNextInt() {
        val prng = createRng()
        prng.nextInt()
        prng.seed = testSeedGenerator.generateSeed(8)
        prng.nextInt()
    }

    @Test(enabled = false)
    @Throws(SeedException::class)
    override fun testRepeatability() {
        // No-op.
    }

    @Test(enabled = false)
    @Throws(SeedException::class)
    override fun testRepeatabilityNextGaussian() {
        // No-op.
    }

    @Throws(SeedException::class)
    override fun createRng(): BaseRandom {
        val wrapper = createRngInternal()
        wrapper.setSeed(SEED_GEN.nextLong())
        return wrapper
    }

    @Throws(SeedException::class)
    override fun createRng(seed: ByteArray?): BaseRandom {
        val wrapper = createRngInternal()
        wrapper.seed = seed
        return wrapper
    }

    /** Assertion-free because SecureRandom isn't necessarily reproducible.  */
    @Test
    override fun testThreadSafety() {
        testThreadSafetyVsCrashesOnly(30,
                ImmutableList.of(BaseRandomTest.NEXT_LONG, BaseRandomTest.NEXT_INT, BaseRandomTest.NEXT_DOUBLE, BaseRandomTest.NEXT_GAUSSIAN, SET_WRAPPED))
    }

    companion object {

        private val SEED_GEN = SecureRandom()
        private val SET_WRAPPED = BaseRandomTest.NamedFunction<Random, Double>({ random ->
            (random as RandomWrapper).wrapped = SecureRandom()
            0.0
        }, "setWrapped")

        private fun createRngInternal(): RandomWrapper {
            try {
                return RandomWrapper(SecureRandom.getInstance("SHA1PRNG"))
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            }

        }
    }
}
