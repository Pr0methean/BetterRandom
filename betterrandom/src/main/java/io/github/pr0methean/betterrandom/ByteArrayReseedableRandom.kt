package io.github.pr0methean.betterrandom

/**
 * A [java.util.Random] that can be reseeded using a byte array instead of using [ ][java.util.Random.setSeed] (although that may also be supported).
 * @author Chris Hennick
 */
interface ByteArrayReseedableRandom /* extends BaseRandom */ {

    /**
     * Returns the preferred length of a new byte-array seed. "Preferred" is implementation-defined
     * when multiple seed lengths are supported, but should probably usually mean the longest one,
     * since the longer the seed, the more random the output.
     * @return The desired length of a new byte-array seed.
     */
    val newSeedLength: Int

    /**
     * Reseed this PRNG.
     * @param seed The PRNG's new seed.
     */
    fun setSeed(seed: ByteArray)

    /**
     * Indicates whether [java.util.Random.setSeed] is recommended over [ ][.setSeed] when the seed is already in the form of a `long`.
     * @return true if [java.util.Random.setSeed] will tend to perform better than [     ][.setSeed].
     */
    open fun preferSeedWithLong(): Boolean {
        return false
    }
}
