package io.github.pr0methean.betterrandom.seed

class FakeSeedGenerator
/**
 * Creates a named instance.
 * @param name the name of this FakeSeedGenerator, returned by [.toString]
 */
@JvmOverloads constructor(private val name: String = "FakeSeedGenerator") : SeedGenerator {

    @Throws(SeedException::class)
    override fun generateSeed(output: ByteArray) {
        // No-op.
    }

    override fun toString(): String {
        return name
    }

    override fun equals(o: Any?): Boolean {
        return this === o || o is FakeSeedGenerator && name == o.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    companion object {

        private val serialVersionUID = 2310664903337315190L
    }
}
