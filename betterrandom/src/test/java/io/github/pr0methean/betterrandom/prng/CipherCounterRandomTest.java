package io.github.pr0methean.betterrandom.prng;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public abstract class CipherCounterRandomTest extends SeekableRandomTest {
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

  @Override protected int getNewSeedLength(final BaseRandom basePrng) {
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

  @SuppressWarnings("ObjectAllocationInLoop") @Override @Test(timeOut = 40_000)
  public void testSetSeedAfterNextLong() throws SeedException {
    if (seedSizeBytes > 16) {
      throw new SkipException("Skipping a redundant test");
    }
    // can't use a real SeedGenerator since we need longs, so use a Random
    final Random masterRNG = new Random();
    final long[] seeds =
        {masterRNG.nextLong(), masterRNG.nextLong(), masterRNG.nextLong(), masterRNG.nextLong()};
    final long otherSeed = masterRNG.nextLong();
    final AesCounterRandom[] rngs = {new AesCounterRandom(getTestSeedGenerator().generateSeed(16)),
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

  @Override @Test(enabled = false) public void testSetSeedAfterNextInt() {
    // No-op.
  }

  @Test(timeOut = 15000) public void testMaxSeedLengthOk() {
    Random rng = createRng();
    if (seedSizeBytes > 16) {
      throw new SkipException("Skipping a redundant test");
    }
    if (!(rng instanceof CipherCounterRandom)) {
      throw new SkipException("Skipping an inapplicable test");
    }
    int max = ((CipherCounterRandom) createRng()).getMaxKeyLengthBytes();
    assert max >= 16 : "Should allow a 16-byte key";
    assert max <= getExpectedMaxSize() :
        "Shouldn't allow a key longer than " + getExpectedMaxSize() + "bytes";
  }

  @Override public void testInitialEntropy() {
    int seedSize = getNewSeedLength(createRng());
    byte[] seed = getTestSeedGenerator().generateSeed(seedSize);
    BaseRandom random = createRng(seed);
    long entropy = random.getEntropyBits();
    assertTrue(entropy > 0, "Initially has zero entropy!");
    if (random instanceof CipherCounterRandom) {
      assertTrue(entropy >= 8 * (seedSizeBytes - ((CipherCounterRandom) random).getCounterSizeBytes()),
          "Initial entropy too low");
    }
    assertTrue(entropy <= 8 * seedSizeBytes, "Initial entropy too high");
  }

  @Override protected abstract BaseRandom createRng() throws SeedException;

  @Override protected abstract BaseRandom createRng(byte[] seed) throws SeedException;
}
