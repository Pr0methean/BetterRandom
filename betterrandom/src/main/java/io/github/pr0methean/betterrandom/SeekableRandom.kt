package io.github.pr0methean.betterrandom

interface SeekableRandom : RepeatableRandom {

    /**
     * Advances the generator forward `delta` steps, but does so in logarithmic time.
     * @param delta the number of steps to advance; can be negative
     */
    fun advance(delta: Long)
}
