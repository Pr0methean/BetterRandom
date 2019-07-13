package io.github.pr0methean.betterrandom.prng;

import static org.testng.Assert.assertTrue;

/**
 * Serves to test the suitability of {@link CipherCounterRandom} for non-AES-based subclassing.
 */
public class TwoFishCounterRandomTest extends CipherCounterRandomTest {
  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return TwoFishCounterRandom.class;
  }

  @Override protected int getExpectedMaxSize() {
    return 96;
  }

  @Override protected BaseRandom createRng() {
    assertTrue(seedSizeBytes > 0, "seedSizeBytes not set");
    return new TwoFishCounterRandom(getTestSeedGenerator().generateSeed(seedSizeBytes));
  }

  @Override protected BaseRandom createRng(byte[] seed) {
    return new TwoFishCounterRandom(seed);
  }
}
