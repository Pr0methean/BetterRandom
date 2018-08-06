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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import io.github.pr0methean.betterrandom.seed.SeedException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import javax.crypto.Cipher;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * Unit test for the AES RNG.
 * @author Daniel Dyer
 * @author Chris Hennick
 */
@Test(testName = "AesCounterRandom")
public class AesCounterRandomTest extends SeekableRandomTest {

  protected int seedSizeBytes;

  private static final int MAX_SIZE;

  static {
    try {
      MAX_SIZE = Cipher.getMaxAllowedKeyLength("AES") / 8
            + AesCounterRandom.COUNTER_SIZE_BYTES;
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }

  /**
   * It'd be more elegant to use a {@code @Factory} static method to set the seed size (which could
   * then be a final field), but that doesn't seem to be compatible with PowerMock; see
   * https://github.com/powermock/powermock/issues/925
   *
   * @param seedSize XML parameter
   */
  @Parameters("seedSize")
  @BeforeClass
  public void setSeedSize(final int seedSize) {
    if (seedSize > MAX_SIZE) {
      assertFalse(seedSize <= 32, "Can't handle a 32-byte seed");
      throw new SkipException("Jurisdiction policy files don't allow this crypto strength");
    }
    seedSizeBytes = seedSize;
  }

  @Override protected int getNewSeedLength(BaseRandom basePrng) {
    return seedSizeBytes;
  }

  @Override @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testSeedTooLong() throws SeedException {
    if (seedSizeBytes > 16) {
      throw new SkipException("Skipping a redundant test");
    }
    createRng(
        getTestSeedGenerator().generateSeed(49)); // Should throw an exception.
  }

  @Override @Test(enabled = false)
  public void testRepeatabilityNextGaussian() throws SeedException {
    // No-op: can't be tested because setSeed merges with the existing seed
  }

  @SuppressWarnings("ObjectAllocationInLoop") @Override @Test(timeOut = 40_000)
  public void testSetSeedAfterNextLong() throws SeedException {
    // can't use a real SeedGenerator since we need longs, so use a Random
    final Random masterRNG = new Random();
    final long[] seeds =
        {masterRNG.nextLong(), masterRNG.nextLong(), masterRNG.nextLong(), masterRNG.nextLong()};
    final long otherSeed = masterRNG.nextLong();
    final AesCounterRandom[] rngs = {
        new AesCounterRandom(getTestSeedGenerator().generateSeed(16)),
        new AesCounterRandom(getTestSeedGenerator().generateSeed(16))};
    for (int i = 0; i < 2; i++) {
      for (final long seed : seeds) {
        final byte[] originalSeed = rngs[i].getSeed();
        assertTrue(originalSeed.length >= 16, "getSeed() returned seed that was too short");
        final AesCounterRandom rngReseeded = new AesCounterRandom(originalSeed);
        final AesCounterRandom rngReseededOther = new AesCounterRandom(originalSeed);
        rngReseeded.setSeed(seed);
        rngReseededOther.setSeed(otherSeed);
        assert !(rngs[i].equals(rngReseeded));
        assert !(rngReseededOther.equals(rngReseeded));
        assert rngs[i].nextLong() != rngReseeded.nextLong() : "setSeed had no effect";
        rngs[i] = rngReseeded;
      }
    }
    assert rngs[0].nextLong() != rngs[1].nextLong() : "RNGs converged after 4 setSeed calls";
  }

  @Override @Test(enabled = false)
  public void testSetSeedAfterNextInt() {
    // No-op.
  }

  @Test(timeOut = 15000) public void testMaxSeedLengthOk() {
    if (seedSizeBytes > 16) {
      throw new SkipException("Skipping a redundant test");
    }
    assert AesCounterRandom.getMaxKeyLengthBytes() >= 16 : "Should allow a 16-byte key";
    assert AesCounterRandom.getMaxKeyLengthBytes() <= 32
        : "Shouldn't allow a key longer than 32 bytes";
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
