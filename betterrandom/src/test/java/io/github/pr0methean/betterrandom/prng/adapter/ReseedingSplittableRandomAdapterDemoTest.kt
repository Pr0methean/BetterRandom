package io.github.pr0methean.betterrandom.prng.adapter

import io.github.pr0methean.betterrandom.seed.SeedException
import org.testng.annotations.Test

class ReseedingSplittableRandomAdapterDemoTest {

    @Test(timeOut = 120000)
    @Throws(SeedException::class, InterruptedException::class)
    fun ensureNoDemoCrash() {
        ReseedingSplittableRandomAdapterDemo.main(NO_ARGS)
    }

    companion object {

        private val NO_ARGS = arrayOf<String>()
    }
}
