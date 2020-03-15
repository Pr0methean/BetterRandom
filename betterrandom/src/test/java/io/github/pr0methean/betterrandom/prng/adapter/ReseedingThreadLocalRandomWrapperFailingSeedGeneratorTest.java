package io.github.pr0methean.betterrandom.prng.adapter;

import static org.testng.Assert.assertEquals;

import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode;
import io.github.pr0methean.betterrandom.seed.FailingSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import org.testng.annotations.Test;

@Test(testName = "ReseedingThreadLocalRandomWrapper:FailingSeedGenerator")
public class ReseedingThreadLocalRandomWrapperFailingSeedGeneratorTest
    extends ReseedingThreadLocalRandomWrapperTest {

  @Override @Test(enabled = false) public void testWrapLegacy() throws SeedException {
    // No-op.
  }

  @Override @Test(enabled = false) public void testSetSeedAfterNextLong() {
    // No-op.
  }

  @Override @Test(enabled = false) public void testSetSeedAfterNextInt() {
    // No-op.
  }

  @Override @Test(enabled = false) public void testSerializable() throws SeedException {
    // No-op.
  }

  @Override @Test(enabled = false) public void testSeedTooLong() throws SeedException {
    // No-op.
  }

  @Override @Test(enabled = false) public void testThreadSafety() {
    // No-op.
  }

  @Override @Test(enabled = false) public void testThreadSafetySetSeed() {
    // No-op.
  }

  @Override @Test(enabled = false) public void testAllPublicConstructors() throws SeedException {
    // No-op.
  }

  @Override protected EntropyCheckMode getEntropyCheckMode() {
    return EntropyCheckMode.EXACT;
  }

  @Override @Test(enabled = false) public void testReseeding() {
    // No-op.
  }

  @Override @Test(enabled = false) public void testSetSeedZero() {
    // No-op.
  }

  @Override @Test(enabled = false) public void testGetWrapped() {
    // No-op.
  }

  @Override public void testInitialEntropy() {
    int seedSize = getNewSeedLength();
    byte[] seed = new byte[seedSize];
    assertEquals(createRng(seed).getEntropyBits(), 8 * seedSize, "Wrong initial entropy");
  }

  @Override protected SeedGenerator getTestSeedGenerator() {
    return FailingSeedGenerator.DEFAULT_INSTANCE;
  }
}
