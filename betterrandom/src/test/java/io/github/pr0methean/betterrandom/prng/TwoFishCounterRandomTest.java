package io.github.pr0methean.betterrandom.prng;

import static org.testng.Assert.assertTrue;

/**
 * Serves to test the suitability of {@link CipherCounterRandom} for non-AES-based subclassing.
 */
public class TwoFishCounterRandomTest extends CipherCounterRandomTest<TwoFishCounterRandom> {
  @Override protected Class<? extends TwoFishCounterRandom> getClassUnderTest() {
    return TwoFishCounterRandom.class;
  }

  @Override protected int getExpectedMaxSize() {
    return 96;
  }

  @Override protected TwoFishCounterRandom createRng() {
    assertTrue(seedSizeBytes > 0, "seedSizeBytes not set");
    return new TwoFishCounterRandom(getTestSeedGenerator().generateSeed(seedSizeBytes));
  }

  @Override protected TwoFishCounterRandom createRng(byte[] seed) {
    return new TwoFishCounterRandom(seed);
  }
}
