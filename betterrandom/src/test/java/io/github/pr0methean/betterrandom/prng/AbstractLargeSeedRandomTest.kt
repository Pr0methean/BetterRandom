package io.github.pr0methean.betterrandom.prng

import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.powermock.api.mockito.PowerMockito.doAnswer
import org.powermock.api.mockito.PowerMockito.`when`

import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator
import io.github.pr0methean.betterrandom.seed.SeedException
import java.lang.reflect.InvocationTargetException
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.mockpolicies.Slf4jMockPolicy
import org.powermock.core.classloader.annotations.MockPolicy
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

/**
 * A subclass of [BaseRandomTest] for when avoiding [io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator]
 * calls is worth the overhead of using PowerMock.
 */
@MockPolicy(Slf4jMockPolicy::class)
@PrepareForTest(DefaultSeedGenerator::class)
@PowerMockIgnore("javax.crypto.*", "javax.management.*", "javax.script.*", "jdk.nashorn.*")
abstract class AbstractLargeSeedRandomTest : BaseRandomTest() {

    private var oldDefaultSeedGenerator: DefaultSeedGenerator? = null

    protected fun mockDefaultSeedGenerator() {
        oldDefaultSeedGenerator = DefaultSeedGenerator.DEFAULT_SEED_GENERATOR
        val mockDefaultSeedGenerator = PowerMockito.mock(DefaultSeedGenerator::class.java)
        `when`(mockDefaultSeedGenerator.generateSeed(anyInt())).thenAnswer { invocation -> BaseRandomTest.SEMIFAKE_SEED_GENERATOR.generateSeed(invocation.getArgument<Any>(0) as Int) }
        doAnswer { invocation ->
            BaseRandomTest.SEMIFAKE_SEED_GENERATOR.generateSeed(invocation.getArgument<Any>(0) as ByteArray)
            null
        }.`when`(mockDefaultSeedGenerator).generateSeed(any(ByteArray::class.java))
        Whitebox.setInternalState(DefaultSeedGenerator::class.java, "DEFAULT_SEED_GENERATOR",
                mockDefaultSeedGenerator)
    }

    protected fun unmockDefaultSeedGenerator() {
        Whitebox.setInternalState(DefaultSeedGenerator::class.java, "DEFAULT_SEED_GENERATOR",
                oldDefaultSeedGenerator)
    }

    @Throws(SeedException::class, IllegalAccessException::class, InstantiationException::class, InvocationTargetException::class)
    override fun testAllPublicConstructors() {
        mockDefaultSeedGenerator()
        try {
            super.testAllPublicConstructors()
        } finally {
            unmockDefaultSeedGenerator()
        }
    }

    override fun testSetSeedLong() {
        mockDefaultSeedGenerator()
        try {
            super.testSetSeedLong()
        } finally {
            unmockDefaultSeedGenerator()
        }
    }
}
