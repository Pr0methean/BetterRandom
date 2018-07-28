package io.github.pr0methean.betterrandom.prng

import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode
import io.github.pr0methean.betterrandom.seed.FailingSeedGenerator
import io.github.pr0methean.betterrandom.seed.SeedException
import java.io.Serializable
import java.util.function.Supplier
import org.testng.annotations.Test

@Test(testName = "ReseedingThreadLocalRandomWrapper:FailingSeedGenerator")
class ReseedingThreadLocalRandomWrapperFailingSeedGeneratorTest : ReseedingThreadLocalRandomWrapperTest() {

    protected override val entropyCheckMode: EntropyCheckMode
        get() = EntropyCheckMode.EXACT

    @Test(enabled = false)
    override fun testReseeding() {
        // No-op.
    }

    @Test(enabled = false)
    override fun testSetSeedZero() {
        // No-op.
    }

    @Throws(SeedException::class)
    override fun createRng(): BaseRandom {
        return ReseedingThreadLocalRandomWrapper(FailingSeedGenerator.FAILING_SEED_GENERATOR,
                pcgSupplier)
    }
}
