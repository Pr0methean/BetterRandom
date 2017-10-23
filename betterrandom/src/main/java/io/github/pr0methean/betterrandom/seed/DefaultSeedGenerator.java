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
    for (final SeedGenerator generator : GENERATORS) {
      if (generator.isWorthTrying()) {
        try {
          generator.generateSeed(output);
          return;
        } catch (final SeedException ignored) {
          // Try the next one
        }
      }
    }
    // This shouldn't happen as at least one the generators should be
    // able to generate a seed.
    throw new SeedException("All available seed generation strategies failed.");
  }

  /**
   * Generates and returns a seed value for a random number generator as a new array.
   * @param length The length of the seed to generate (in bytes).
   * @return A byte array containing the seed data.
   * @throws SeedException If a seed cannot be generated for any reason.
   */
  @Override public byte[] generateSeed(final int length) throws SeedException {
    if (length <= 0) {
      return EMPTY_SEED;
    }
    final byte[] output = new byte[length];
    generateSeed(output);
    return output;
  }

  /**
   * Returns true if we cannot determine quickly (i.e. without I/O calls) that this SeedGenerator
   * would throw a {@link SeedException} if {@link #generateSeed(int)} or {@link
   * #generateSeed(byte[])} were being called right now.
   * @return true if this SeedGenerator will get as far as an I/O call or other slow operation in
   *     attempting to generate a seed immediately.
   */
  @Override public boolean isWorthTrying() {
    return true;
  }
}
