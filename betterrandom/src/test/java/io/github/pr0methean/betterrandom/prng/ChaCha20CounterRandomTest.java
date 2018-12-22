package io.github.pr0methean.betterrandom.prng;

import org.junit.Test;

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
  @Test
  public void testAdvanceForward(int delta) {
    super.testAdvanceForward(delta);
  }

  @Override
  @Test
  public void testAdvanceZero() {
    super.testAdvanceZero();
  }

  @Override
  public void testNextInt() {
    super.testNextInt();
  }

  @Override
  @Test
  public void testAdvanceBackward(int delta) {
    super.testAdvanceBackward(delta);
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
