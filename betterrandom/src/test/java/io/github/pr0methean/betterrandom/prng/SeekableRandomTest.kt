package io.github.pr0methean.betterrandom.prng

import io.github.pr0methean.betterrandom.SeekableRandom
import java.util.Random
import org.testng.annotations.Test

/**
 * Abstract test class for a class that implements [SeekableRandom].
 */
abstract class SeekableRandomTest : BaseRandomTest() {

    @Test
    open fun testAdvanceForward() {
        for (i in 0 until ITERATIONS) {
            val copy1AsRandom = createRng()
            val copy1 = copy1AsRandom as SeekableRandom
            val copy2AsRandom = createRng(copy1.seed)
            val copy2 = copy2AsRandom as SeekableRandom
            for (j in 0 until DELTA) {
                copy1AsRandom.nextInt()
            }
            copy2.advance(DELTA.toLong())
            RandomTestUtils.assertEquivalent(copy1AsRandom, copy2AsRandom, DELTA,
                    "Output mismatch after advancing forward")
        }
    }

    @Test
    open fun testAdvanceZero() {
        val copy1AsRandom = createRng()
        val copy1 = copy1AsRandom as SeekableRandom
        val copy2AsRandom = createRng(copy1.seed)
        val copy2 = copy2AsRandom as SeekableRandom
        copy2.advance(0)
        RandomTestUtils.assertEquivalent(copy1AsRandom, copy2AsRandom, DELTA,
                "Output mismatch after advancing by zero")
    }

    @Test
    open fun testAdvanceBackward() {
        for (i in 0 until ITERATIONS) {
            val copy1AsRandom = createRng()
            val copy1 = copy1AsRandom as SeekableRandom
            val copy2AsRandom = createRng(copy1.seed)
            for (j in 0 until DELTA) {
                copy1AsRandom.nextInt()
            }
            copy1.advance((-DELTA).toLong())
            RandomTestUtils.assertEquivalent(copy1AsRandom, copy2AsRandom, DELTA,
                    "Output mismatch after advancing backward")
        }
    }

    companion object {

        private val ITERATIONS = 10 // because bugs may depend on the initial seed value
        private val DELTA = 37
    }
}
