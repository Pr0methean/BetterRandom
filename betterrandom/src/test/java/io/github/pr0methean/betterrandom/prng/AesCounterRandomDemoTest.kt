package io.github.pr0methean.betterrandom.prng

import io.github.pr0methean.betterrandom.seed.RandomDotOrgUtils
import io.github.pr0methean.betterrandom.seed.SeedException
import org.testng.annotations.Test

@Test(testName = "AesCounterRandomDemo")
class AesCounterRandomDemoTest {

    @Test(timeOut = 120000)
    @Throws(SeedException::class)
    fun ensureNoDemoCrash() {
        if (RandomDotOrgUtils.canRunRandomDotOrgLargeTest()) {
            AesCounterRandomDemo.main(NO_ARGS)
        }
    }

    companion object {

        private val NO_ARGS = arrayOf<String>()
    }
}
