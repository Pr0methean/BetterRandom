package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.Pcg64Random;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode;
import io.github.pr0methean.betterrandom.seed.FailingSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
<<<<<<< HEAD:betterrandom/src/test/java/io/github/pr0methean/betterrandom/prng/ReseedingThreadLocalRandomWrapperFailingSeedGeneratorTest.java
=======
import java.io.Serializable;
import java.util.function.Supplier;
>>>>>>> master:betterrandom/src/test/java/io/github/pr0methean/betterrandom/prng/adapter/ReseedingThreadLocalRandomWrapperFailingSeedGeneratorTest.java
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

  @Override protected SeedGenerator getTestSeedGenerator() {
    return FailingSeedGenerator.FAILING_SEED_GENERATOR;
  }
}
