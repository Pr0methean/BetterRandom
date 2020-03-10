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
package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedException;
import org.testng.annotations.Test;

/**
 * Unit test for the Complementary Multiply With Carry (CMWC) RNG.
 *
 * @author Daniel Dyer
 * @author Chris Hennick
 */
@Test(testName = "Cmwc4096Random") public class Cmwc4096RandomTest
    extends BaseRandomTest<Cmwc4096Random> {

  @Override protected Class<? extends Cmwc4096Random> getClassUnderTest() {
    return Cmwc4096Random.class;
  }

  @Override protected Cmwc4096Random createRng() throws SeedException {
    return new Cmwc4096Random(getTestSeedGenerator());
  }

  @Override protected Cmwc4096Random createRng(final byte[] seed) throws SeedException {
    return new Cmwc4096Random(seed);
  }

  @Override @Test(enabled = false) public void testRandomSeederIntegration() {
    // No-op: can't be made to reliably finish in time because the seed is too large.
  }
}
