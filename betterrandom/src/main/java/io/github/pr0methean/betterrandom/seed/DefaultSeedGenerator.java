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
 * <p>
 * Seed generator that is the default for the program where it is running. PRNGs that are serialized
 * using one program's default and deserialized in another program will use the latter's default
 * seed generator, which is part of the static state. This means a PRNG's serial form won't specify
 * a particular seed generator if it was constructed without specifying one.
 * </p><p>
 * The default implementation maintains multiple strategies for seed generation and will delegate to
 * the best one available at any moment. It uses, in order of preference:</p><ol>
 * <li>{@link DevRandomSeedGenerator} with 128-byte buffer</li>
 * <li>{@link RandomDotOrgSeedGenerator#DELAYED_RETRY} with 625-byte buffer</li>
 * <li>{@link SecureRandomSeedGenerator} with no buffer</li>
 * </ol>
 *
 * @author Daniel Dyer
 */
public enum DefaultSeedGenerator implements SeedGenerator {

  /**
   * Singleton instance.
   */
  DEFAULT_SEED_GENERATOR;

  private static volatile SeedGenerator delegate = new SeedGeneratorPreferenceList(true,
      new BufferedSeedGenerator(DevRandomSeedGenerator.DEV_RANDOM_SEED_GENERATOR, 128),
      new BufferedSeedGenerator(RandomDotOrgSeedGenerator.DELAYED_RETRY, 625),
      SecureRandomSeedGenerator.DEFAULT_INSTANCE);

  /**
   * Returns the current delegate used by this class's singleton instance.
   *
   * @return the current delegate
   */
  public static SeedGenerator get() {
    return delegate;
  }

  /**
   * Sets the default seed generator (a delegate used by this class's singleton instance).
   *
   * @param delegate the new delegate
   */
  public static void set(SeedGenerator delegate) {
    if (delegate == null) {
      throw new IllegalArgumentException("Can't set the default seed generator to null");
    }
    DefaultSeedGenerator.delegate = delegate;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Generates a seed by trying each of the available strategies in turn until one succeeds.  Tries
   * the most suitable strategy first and eventually degrades to the least suitable (but guaranteed
   * to work) strategy.
   */
  @Override public void generateSeed(final byte[] output) throws SeedException {
    delegate.generateSeed(output);
  }

  @Override public byte[] generateSeed(int length) throws SeedException {
    return delegate.generateSeed(length);
  }
}
