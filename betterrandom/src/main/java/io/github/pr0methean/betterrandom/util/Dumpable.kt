package io.github.pr0methean.betterrandom.util

/**
 * Object that can be dumped (written for debugging purposes to a more detailed string
 * representation than what [Object.toString] returns).
 * @author Chris Hennick
 */
interface Dumpable {

    /**
     * Returns a [String] representing the state of this object for debugging purposes,
     * including mutable state that [Object.toString] usually doesn't return.
     * @return a representation of this object and its state.
     */
    @EntryPoint
    fun dump(): String
}
