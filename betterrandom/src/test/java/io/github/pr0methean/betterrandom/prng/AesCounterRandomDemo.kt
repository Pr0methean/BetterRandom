package io.github.pr0methean.betterrandom.prng

import io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR

import io.github.pr0methean.betterrandom.seed.RandomSeederThread
import io.github.pr0methean.betterrandom.seed.SeedException
import io.github.pr0methean.betterrandom.util.BinaryUtils

enum class AesCounterRandomDemo {
    ;

    companion object {

        @Throws(SeedException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val random = AesCounterRandom(SECURE_RANDOM_SEED_GENERATOR)
            RandomSeederThread.add(SECURE_RANDOM_SEED_GENERATOR, random)
            val randomBytes = ByteArray(32)
            for (i in 0..19) {
                random.nextBytes(randomBytes)
                System.out.format("Bytes: %s%n", BinaryUtils.convertBytesToHexString(randomBytes))
            }
        }
    }
}
