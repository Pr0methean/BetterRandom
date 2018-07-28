package io.github.pr0methean.betterrandom.prng

import io.github.pr0methean.betterrandom.seed.RandomSeederThread
import io.github.pr0methean.betterrandom.seed.SeedException
import io.github.pr0methean.betterrandom.seed.SeedGenerator
import io.github.pr0methean.betterrandom.util.BinaryUtils
import java.io.Serializable
import java.util.Random
import java.util.function.Function
import java.util.function.LongFunction
import java.util.function.Supplier

/**
 * A [ThreadLocalRandomWrapper] that reseeds all its instances using the
 * [RandomSeederThread] for its [SeedGenerator].
 */
class ReseedingThreadLocalRandomWrapper : ThreadLocalRandomWrapper {

    /**
     * Wraps the given [Supplier]. Uses the given [RandomSeederThread] to reseed PRNGs,
     * but not to initialize them unless the [Supplier] does so. This ThreadLocalRandomWrapper
     * will be serializable if the [Supplier] is serializable.
     * @param initializer a supplier that will be called to provide the initial [BaseRandom]
     * for each thread.
     * @param seedGenerator The seed generation strategy whose [RandomSeederThread] will be
     * used to reseed each thread's PRNG.
     */
    @Throws(SeedException::class)
    constructor(seedGenerator: SeedGenerator,
                initializer: Supplier<out BaseRandom>) : super({
        val out = initializer.get()
        out.setSeedGenerator(seedGenerator)
        out
    } as Serializable) {
    }

    /**
     * Wraps a seed generator and a function that takes a seed byte array as input. This
     * ReseedingThreadLocalRandomWrapper will be serializable if the [Function] is
     * serializable.
     * @param seedSize the size of seed arrays to generate.
     * @param seedGenerator The seed generation strategy that will provide the seed value for each
     * thread's [BaseRandom], both at initialization and through the corresponding [     ].
     * @param creator a [Function] that creates a [BaseRandom] from each seed.
     * Probably a constructor reference.
     */
    @Throws(SeedException::class)
    constructor(seedSize: Int, seedGenerator: SeedGenerator,
                creator: Function<ByteArray, out BaseRandom>) : super(seedSize, seedGenerator,
            { seed ->
                val out = creator.apply(seed)
                out.setSeedGenerator(seedGenerator)
                out
            } as Serializable) {
    }

    companion object {

        private val serialVersionUID = -3235519018032714059L

        /**
         * Uses this class and [RandomWrapper] to decorate any implementation of [Random] that
         * can be constructed from a `long` seed into a fully-concurrent one.
         * @param legacyCreator a function that provides the [Random] that underlies the
         * returned wrapper on each thread, taking a seed as input.
         * @param seedGenerator the seed generator whose output will be fed to `legacyCreator`.
         * @return a ThreadLocalRandomWrapper decorating instances created by `legacyCreator`.
         */
        override fun wrapLegacy(
                legacyCreator: LongFunction<Random>, seedGenerator: SeedGenerator): ReseedingThreadLocalRandomWrapper {
            return ReseedingThreadLocalRandomWrapper(java.lang.Long.BYTES, seedGenerator,
                    { bytes -> RandomWrapper(legacyCreator.apply(BinaryUtils.convertBytesToLong(bytes))) })
        }
    }
}
