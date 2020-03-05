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


import static io.github.pr0methean.betterrandom.TestUtils.fail;

import io.github.pr0methean.betterrandom.seed.SeedException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import org.testng.annotations.Test;

/**
 * Unit test for the AES RNG.
 *
 * @author Daniel Dyer
 * @author Chris Hennick
 */
@Test(testName = "AesCounterRandom") public class AesCounterRandomTest
    extends CipherCounterRandomTest {

  private static final int MAX_SIZE;

  static {
    try {
      int maxKeySize = Math.min(Cipher.getMaxAllowedKeyLength("AES") / 8, 32);
      MAX_SIZE = maxKeySize + CipherCounterRandom.DEFAULT_COUNTER_SIZE_BYTES;
    } catch (final NoSuchAlgorithmException e) {
      throw fail("NoSuchAlgorithmException should not occur for AES", e);
    }
  }

  @Override protected int getExpectedMaxSize() {
    return MAX_SIZE;
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return AesCounterRandom.class;
  }

  @Override protected BaseRandom createRng() throws SeedException {
    return new AesCounterRandom(getTestSeedGenerator().generateSeed(seedSizeBytes));
  }

  @Override protected BaseRandom createRng(final byte[] seed) throws SeedException {
    return new AesCounterRandom(seed);
  }
}
