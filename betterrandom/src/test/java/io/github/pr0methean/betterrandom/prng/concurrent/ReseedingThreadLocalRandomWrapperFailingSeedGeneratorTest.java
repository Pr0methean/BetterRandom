package io.github.pr0methean.betterrandom.prng.concurrent;

import static org.testng.Assert.assertEquals;

import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode;
import io.github.pr0methean.betterrandom.seed.FailingSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import org.testng.annotations.Test;

@Test(testName = "ReseedingThreadLocalRandomWrapper:FailingSeedGenerator")
public class ReseedingThreadLocalRandomWrapperFailingSeedGeneratorTest
    extends ReseedingThreadLocalRandomWrapperTest {

  public ReseedingThreadLocalRandomWrapperFailingSeedGeneratorTest() {
    pcgSupplier = new Pcg64RandomColonColonNew(semiFakeSeedGenerator);
  }

  @Override @Test(enabled = false) public void testWrapLegacy() {
    // No-op.
  }

  @Override @Test(enabled = false) public void testSetSeedAfterNextLong() {
    // No-op.
  }

  @Override @Test(enabled = false) public void testSetSeedAfterNextInt() {
    // No-op.
  }

  @Override @Test(enabled = false) public void testSerializable() {
    // No-op.
  }

  @Override @Test(enabled = false) public void testSeedTooLong() {
    // No-op.
  }

  @Override @Test(enabled = false) public void testThreadSafety() {
    // No-op.
  }

  @Override @Test(enabled = false) public void testThreadSafetySetSeed() {
    // No-op.
  }

  @Override @Test(enabled = false) public void testAllPublicConstructors() {
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
    int seedSize = getNewSeedLength(createRng());
    byte[] seed = new byte[seedSize];
    assertEquals(createRng(seed).getEntropyBits(), 8 * seedSize, "Wrong initial entropy");
  }

  @Override protected SeedGenerator getTestSeedGenerator() {
    return FailingSeedGenerator.DEFAULT_INSTANCE;
  }
}
