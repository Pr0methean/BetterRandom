package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.TestUtils.assertGreaterOrEqual;
import static io.github.pr0methean.betterrandom.TestUtils.assertLessOrEqual;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public abstract class CipherCounterRandomTest<T extends CipherCounterRandom>
    extends SeekableRandomTest<T> {
  protected abstract int getExpectedMaxSize();

  protected int seedSizeBytes;

  /**
   * It'd be more elegant to use a {@code @Factory} static method to set the seed size (which could
   * then be a final field), but that doesn't seem to be compatible with PowerMock; see
   * https://github.com/powermock/powermock/issues/925
   *
   * @param seedSize XML parameter
   */
  @Parameters("seedSize") @BeforeClass public void setSeedSize(@Optional("16") final int seedSize) {
    if (seedSize > getExpectedMaxSize()) {
      assertFalse(seedSize <= 32, "Can't handle a 32-byte seed");
      throw new SkipException(
          "Test can't run without jurisdiction policy files that allow larger AES keys");
    }
    seedSizeBytes = seedSize;
  }

  @Override protected int getNewSeedLength() {
    return seedSizeBytes;
  }

  @Override @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testSeedTooLong() throws SeedException {
    if (seedSizeBytes > 16) {
      throw new SkipException("Skipping a redundant test");
    }
    createRng(getTestSeedGenerator()
        .generateSeed(getExpectedMaxSize() + 1)); // Should throw an exception.
  }

  @Override @Test(enabled = false) public void testRepeatabilityNextGaussian()
      throws SeedException {
    // No-op: can't be tested because setSeed merges with the existing seed
  }

  @Override @Test(timeOut = 40_000)
  public void testSetSeedAfterNextLong() throws SeedException {
    if (seedSizeBytes > 16) {
      throw new SkipException("Skipping a redundant test");
    }
    checkSetSeedForCipher(this);
  }

  public static void checkSetSeedForCipher(
      BaseRandomTest<?> test) {
    // can't use a real SeedGenerator since we need longs, so use a Random
    final Random masterRNG = new Random();
    final long[] seeds =
        {masterRNG.nextLong(), masterRNG.nextLong(), masterRNG.nextLong(), masterRNG.nextLong()};
    final long otherSeed = masterRNG.nextLong();
    final BaseRandom[] rngs = {
        test.createRng(test.getTestSeedGenerator().generateSeed(16)),
        test.createRng(test.getTestSeedGenerator().generateSeed(16))};
    for (int i = 0; i < 2; i++) {
      for (final long seed : seeds) {
        final byte[] originalSeed = rngs[i].getSeed();
        assertTrue(originalSeed.length >= 16, "getSeed() returned seed that was too short");
        final BaseRandom rngReseeded = test.createRng(originalSeed);
        final BaseRandom rngReseededOther = test.createRng(originalSeed);
        rngReseeded.setSeed(seed);
        rngReseededOther.setSeed(otherSeed);
        assertNotEquals(rngs[i], rngReseeded, "PRNG equals() one with a different seed");
        assertNotEquals(rngReseededOther, rngReseeded, "PRNG equals() one with a different seed");
        assertNotEquals(rngs[i].nextLong(), rngReseeded.nextLong(), "setSeed had no effect");
        rngs[i] = rngReseeded;
      }
    }
    assertNotEquals(rngs[0].nextLong(), rngs[1].nextLong(), "RNGs converged after 4 setSeed calls");
  }

  @Override @Test(enabled = false) public void testSetSeedAfterNextInt() {
    // No-op.
  }

  @Test(timeOut = 15000) public void testMaxSeedLengthOk() {
    if (seedSizeBytes > 16) {
      throw new SkipException("Skipping a redundant test");
    }
    int max = createRng().getMaxKeyLengthBytes();
    assertGreaterOrEqual(max, 16, "Should allow a 16-byte key");
    assertLessOrEqual(max, getExpectedMaxSize(),
        "Shouldn't allow a key longer than " + getExpectedMaxSize() + "bytes");
  }

  @Override public void testInitialEntropy() {
    checkInitialEntropyForCipher(this, createRng().getCounterSizeBytes());
  }

  public static <T extends BaseRandom> void checkInitialEntropyForCipher(
      BaseRandomTest<T> test, final int counterSizeBytes) {
    int seedSize = test.getNewSeedLength();
    byte[] seed = test.getTestSeedGenerator().generateSeed(seedSize);
    T random = test.createRng(seed);
    long entropy = random.getEntropyBits();
    assertTrue(entropy > 0, "Initially has zero entropy!");
    assertTrue(entropy >= 8 * (seedSize - counterSizeBytes),
        "Initial entropy too low");
    assertTrue(entropy <= 8 * seedSize, "Initial entropy too high");
  }

  @Override protected abstract T createRng() throws SeedException;

  @Override protected abstract T createRng(byte[] seed) throws SeedException;
}
