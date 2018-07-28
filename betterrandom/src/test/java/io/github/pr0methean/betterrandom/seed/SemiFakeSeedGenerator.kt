package io.github.pr0methean.betterrandom.seed

import java.util.Random

class SemiFakeSeedGenerator(private val random: Random) : SeedGenerator {

    @Throws(SeedException::class)
    override fun generateSeed(output: ByteArray) {
        random.nextBytes(output)
    }

    override fun equals(o: Any?): Boolean {
        return this === o || o is SemiFakeSeedGenerator && random == o.random
    }

    override fun hashCode(): Int {
        return random.hashCode()
    }

    companion object {

        private val serialVersionUID = 3490669976564244209L
    }
}
