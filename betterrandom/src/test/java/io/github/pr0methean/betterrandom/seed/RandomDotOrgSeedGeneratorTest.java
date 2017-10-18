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

import static io.github.pr0methean.betterrandom.TestUtils.canRunRandomDotOrgLargeTest;
import static io.github.pr0methean.betterrandom.TestUtils.isNotAppveyor;
import static org.testng.Assert.assertEquals;

import java.util.UUID;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Unit test for the seed generator that connects to random.org to get seed data.
 *
 * @author Daniel Dyer
 * @author Chris Hennick
 */
public class RandomDotOrgSeedGeneratorTest {

  private static void setApiKey() {
    String apiKeyString = System.getenv("RANDOM_DOT_ORG_KEY");
    RandomDotOrgSeedGenerator.setApiKey((apiKeyString == null) ? null : UUID.fromString(apiKeyString));
  }

  @Test(timeOut = 120000)
  public void testGeneratorOldApi() throws SeedException {
    if (isNotAppveyor()) {
      RandomDotOrgSeedGenerator.setApiKey(null);
      SeedTestUtils.testGenerator(RandomDotOrgSeedGenerator.RANDOM_DOT_ORG_SEED_GENERATOR);
    }
  }

  @Test(timeOut = 120000)
  public void testGeneratorNewApi() throws SeedException {
    setApiKey();
    SeedTestUtils.testGenerator(RandomDotOrgSeedGenerator.RANDOM_DOT_ORG_SEED_GENERATOR);
  }

  /**
   * Try to acquire a large number of bytes, more than are cached internally by the seed generator
   * implementation.
   */
  @Test(timeOut = 120000)
  public void testLargeRequest() throws SeedException {
    setApiKey();
    // Request more bytes than are cached internally.
    final int seedLength = 626;
    final SeedGenerator generator = RandomDotOrgSeedGenerator.RANDOM_DOT_ORG_SEED_GENERATOR;
    assertEquals(generator.generateSeed(seedLength).length, seedLength,
        "Failed to generate seed of length " + seedLength);
  }
}
