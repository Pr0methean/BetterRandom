package io.github.pr0methean.betterrandom.prng.adapter

import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator
import io.github.pr0methean.betterrandom.seed.SeedException
import io.github.pr0methean.betterrandom.seed.SeedGenerator
import java.io.IOException
import java.io.ObjectInputStream
import java.util.SplittableRandom

/**
 * Simple, non-thread-safe implementation of [io.github.pr0methean.betterrandom.prng.BaseRandom]
 * that wraps a [SplittableRandom].
 * @author Chris Hennick
 */
class SingleThreadSplittableRandomAdapter : DirectSplittableRandomAdapter {

    /**
     * Returns this SingleThreadSplittableRandomAdapter's only [SplittableRandom].
     * @return [.underlying]
     */
    override val splittableRandom: SplittableRandom
        get() = underlying

    /**
     * Use the provided seed generation strategy to create the seed for the underlying [ ].
     * @param seedGenerator The seed generation strategy that will provide the seed value for this
     * RNG.
     * @throws SeedException if there is a problem generating a seed.
     */
    @Throws(SeedException::class)
    constructor(seedGenerator: SeedGenerator) : this(seedGenerator.generateSeed(java.lang.Long.BYTES)) {
    }

    /**
     * Use the provided seed for the underlying [SplittableRandom].
     * @param seed The seed. Must be 8 bytes.
     */
    @JvmOverloads constructor(seed: ByteArray = DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(java.lang.Long.BYTES)) : super(seed) {}

    /**
     * Use the provided seed for the underlying [SplittableRandom].
     * @param seed The seed.
     */
    constructor(seed: Long) : super(seed) {}

    /**
     * Must be redeclared in this package so that [ReseedingSplittableRandomAdapter] can access
     * it.
     */
    public override fun debitEntropy(bits: Long) {
        super.debitEntropy(bits)
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(`in`: ObjectInputStream) {
        `in`.defaultReadObject()
        setSeed(seed)
    }

    companion object {

        private val serialVersionUID = -1125374167384636394L
    }
}
/**
 * Use the [DefaultSeedGenerator] to create the seed for the underlying [ ].
 * @throws SeedException if the [DefaultSeedGenerator] fails to generate a seed.
 */
