package io.github.pr0methean.betterrandom.prng;

import static org.testng.Assert.assertTrue;

import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import org.testng.SkipException;
import org.testng.annotations.Test;

public abstract class AbstractAesCounterRandomTest extends SeekableRandomTest {

  protected final int seedSizeBytes;

  public AbstractAesCounterRandomTest(int seedSizeBytes) {
    this.seedSizeBytes = seedSizeBytes;
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
    final AesCounterRandom[] rngs = {new AesCounterRandom(16), new AesCounterRandom(16)};
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
