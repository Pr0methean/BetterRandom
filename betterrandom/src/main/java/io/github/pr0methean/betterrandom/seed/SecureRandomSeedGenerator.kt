// ============================================================================
//   Copyright 2006-2012 Daniel W. Dyer
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
// ============================================================================
package io.github.pr0methean.betterrandom.seed

import java.security.SecureRandom

/**
 *
 * [SeedGenerator] implementation that uses Java's bundled [SecureRandom] RNG to
 * generate random seed data.
 *
 *The advantage of using SecureRandom for seeding but not as the
 * primary RNG is that we can use it to seed RNGs that are much faster than SecureRandom.
 *
 * This is the only seeding strategy that is guaranteed to work on all platforms and therefore
 * is
 * provided as a fall-back option should none of the other provided [SeedGenerator]
 * implementations be usable.
 *
 *On Oracle and OpenJDK, SecureRandom uses [`sun.security.provider.SeedGenerator`](http://hg.openjdk.java.net/jdk8/jdk8/jdk/file/tip/src/share/classes/sun/security/provider/SeedGenerator.java); when `/dev/random` isn't available, that
 * SeedGenerator class in turn uses the timing of newly-launched threads as a source of randomness,
 * relying on the unpredictable interactions between different configurations of hardware and
 * software and their workloads.
 * @author Daniel Dyer
 */
enum class SecureRandomSeedGenerator : SeedGenerator {

    SECURE_RANDOM_SEED_GENERATOR;

    override fun generateSeed(length: Int): ByteArray {
        return if (length <= 0) {
            SeedGenerator.EMPTY_SEED
        } else SOURCE.generateSeed(length)
    }

    override fun generateSeed(output: ByteArray) {
        System.arraycopy(SOURCE.generateSeed(output.size), 0, output, 0, output.size)
    }

    override fun toString(): String {
        return "java.security.SecureRandom"
    }

    companion object {

        /** The [SecureRandom] that generates the seeds.  */
        private val SOURCE = SecureRandom()
    }
}
