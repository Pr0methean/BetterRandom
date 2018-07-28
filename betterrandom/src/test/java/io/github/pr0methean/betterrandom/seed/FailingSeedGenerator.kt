package io.github.pr0methean.betterrandom.seed

/**
 * A [SeedGenerator] that always throws a [SeedException] when asked to generate a seed.
 */
enum class FailingSeedGenerator : SeedGenerator {
    FAILING_SEED_GENERATOR;

    override val isWorthTrying: Boolean
        get() = false

    @Throws(SeedException::class)
    override fun generateSeed(output: ByteArray) {
        throw SeedException("This is the FailingSeedGenerator")
    }

}
