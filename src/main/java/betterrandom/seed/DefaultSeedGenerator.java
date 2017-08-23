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
package betterrandom.seed;

/**
 * Seed generator that maintains multiple strategies for seed generation and will delegate to the
 * best one available for the current operating environment.
 *
 * @author Daniel Dyer
 */
public final class DefaultSeedGenerator implements SeedGenerator {

  /**
   * Singleton instance.
   */
  private static final DefaultSeedGenerator INSTANCE = new DefaultSeedGenerator();

  /**
   * Delegate generators.
   */
  private static final SeedGenerator[] GENERATORS = {
      DevRandomSeedGenerator.getInstance(),
      RandomDotOrgSeedGenerator.getInstance(),
      SecureRandomSeedGenerator.getInstance()
  };
  private static final long serialVersionUID = -755783059285108828L;


  private DefaultSeedGenerator() {
    // Private constructor prevents external instantiation.
  }


  /**
   * @return The singleton instance of this class.
   */
  public static DefaultSeedGenerator getInstance() {
    return INSTANCE;
  }


  /**
   * Generates a seed by trying each of the available strategies in turn until one succeeds.  Tries
   * the most suitable strategy first and eventually degrades to the least suitable (but guaranteed
   * to work) strategy.
   *
   * @param length The length (in bytes) of the seed.
   * @return A random seed of the requested length.
   */
  @Override
  public byte[] generateSeed(int length) throws SeedException {
    for (SeedGenerator generator : GENERATORS) {
      try {
        return generator.generateSeed(length);
      } catch (SeedException ex) {
        // Ignore and try the next generator...
      }
    }
    // This shouldn't happen as at least one the generators should be
    // able to generate a seed.
    throw new SeedException("All available seed generation strategies failed.");
  }
  
  @Override
  public boolean equals(Object o) {
    return o != null && getClass() == o.getClass();
  }
  
  @Override
  public int hashCode() {
    return -775963758;
  }
}
