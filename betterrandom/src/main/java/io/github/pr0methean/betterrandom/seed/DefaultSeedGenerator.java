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
package io.github.pr0methean.betterrandom.seed;

/**
 * Seed generator that maintains multiple strategies for seed generation and will delegate to the
 * best one available at any moment. Uses, in order of preference: <ol> <li>{@link
 * DevRandomSeedGenerator}</li> <li>{@link RandomDotOrgSeedGenerator#DELAYED_RETRY}</li> <li>{@link
 * SecureRandomSeedGenerator}</li> </ol>
 * @author Daniel Dyer
 */
public enum DefaultSeedGenerator implements SeedGenerator {

  /**
   * Singleton instance.
   */
  DEFAULT_SEED_GENERATOR;

  /**
   * Delegate generators.
   */
  private static final SeedGenerator[] GENERATORS =
      {DevRandomSeedGenerator.DEV_RANDOM_SEED_GENERATOR, RandomDotOrgSeedGenerator.DELAYED_RETRY,
          SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR};

  /**
   * {@inheritDoc}
   * <p>
   * Generates a seed by trying each of the available strategies in turn until one succeeds.  Tries
   * the most suitable strategy first and eventually degrades to the least suitable (but guaranteed
   * to work) strategy.
   */
  @Override public void generateSeed(final byte[] output) throws SeedException {
    throw new AssertionError("DefaultSeedGenerator called");
  }
}
