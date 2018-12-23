package io.github.pr0methean.betterrandom.prng;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 * Serves to test the suitability of {@link CipherCounterRandom} for non-AES-based subclassing.
 */
public class ChaCha20CounterRandomTest extends CipherCounterRandomTest {
  @Override
  protected Class<? extends BaseRandom> getClassUnderTest() {
    return ChaCha20CounterRandom.class;
  }

  @Override
  protected int getExpectedMaxSize() {
    return 96;
  }

  @Override
  protected BaseRandom createRng() {
    assertTrue(seedSizeBytes > 0, "seedSizeBytes not set");
    return new ChaCha20CounterRandom(getTestSeedGenerator().generateSeed(seedSizeBytes));
  }

  @Override
  protected BaseRandom createRng(byte[] seed) {
    return new ChaCha20CounterRandom(seed);
  }
}
