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

import static io.github.pr0methean.betterrandom.seed.RandomDotOrgSeedGenerator.RANDOM_DOT_ORG_SEED_GENERATOR;
import static io.github.pr0methean.betterrandom.seed.RandomDotOrgSeedGenerator.setApiKey;
import static io.github.pr0methean.betterrandom.seed.RandomDotOrgUtils.haveApiKey;
import static io.github.pr0methean.betterrandom.seed.RandomDotOrgUtils.setApiKey;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Unit test for the seed generator that connects to random.org to get seed data.
 *
 * @author Daniel Dyer
 * @author Chris Hennick
 */
@Test(singleThreaded = true) public class RandomDotOrgSeedGeneratorLiveTest
    extends WebJsonSeedGeneratorLiveTest<RandomDotOrgSeedGenerator> {

  public RandomDotOrgSeedGeneratorLiveTest() {
    super(RANDOM_DOT_ORG_SEED_GENERATOR);
  }

  @Test(timeOut = 120000) public void testGeneratorOldApi() throws SeedException {
    setApiKey(null);
    SeedTestUtils.testGenerator(seedGenerator, true);
  }

  @Test(timeOut = 120000) public void testGeneratorNewApi() throws SeedException {
    if (haveApiKey()) {
      setApiKey();
      SeedTestUtils.testGenerator(seedGenerator, true);
    } else {
      throw new SkipException("Test can't run on this platform");
    }
  }

  @Override public void testToString() {
    super.testToString();
    Assert.assertNotNull(RandomDotOrgSeedGenerator.DELAYED_RETRY.toString());
  }

  @AfterMethod public void tearDownMethod() {
    setApiKey(null);
  }

  @Override public void testSetProxyReal() {
    if (haveApiKey()) {
      setApiKey();
    }
    super.testSetProxyReal();
  }
}
