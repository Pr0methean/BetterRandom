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

/**
 * Seed generator that maintains multiple strategies for seed generation and will delegate to the
 * best one available at any moment. Uses, in order of preference:   1. [ ]  1. [RandomDotOrgSeedGenerator.DELAYED_RETRY]  1. [ ]
 * @author Daniel Dyer
 */
enum class DefaultSeedGenerator : SeedGenerator {

    /**
     * Singleton instance.
     */
    DEFAULT_SEED_GENERATOR;

    /**
     * {@inheritDoc}
     *
     *
     * Generates a seed by trying each of the available strategies in turn until one succeeds.  Tries
     * the most suitable strategy first and eventually degrades to the least suitable (but guaranteed
     * to work) strategy.
     */
    @Throws(SeedException::class)
    override fun generateSeed(output: ByteArray) {
        for (generator in GENERATORS) {
            if (generator.isWorthTrying) {
                try {
                    generator.generateSeed(output)
                    return
                } catch (ignored: SeedException) {
                    // Try the next one
                }

            }
        }
        // This shouldn't happen as at least one the generators should be
        // able to generate a seed.
        throw SeedException("All available seed generation strategies failed.")
    }

    companion object {

        /**
         * Delegate generators.
         */
        private val GENERATORS = arrayOf<SeedGenerator>(DevRandomSeedGenerator.DEV_RANDOM_SEED_GENERATOR, RandomDotOrgSeedGenerator.DELAYED_RETRY, SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR)
    }
}
