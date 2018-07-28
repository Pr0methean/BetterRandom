package io.github.pr0methean.betterrandom.seed

import org.powermock.modules.testng.PowerMockTestCase
import org.testng.Assert
import org.testng.annotations.Test

abstract class AbstractSeedGeneratorTest protected constructor(protected val seedGenerator: SeedGenerator) : PowerMockTestCase() {

    @Test
    open fun testToString() {
        Assert.assertNotNull(seedGenerator.toString())
    }
}
