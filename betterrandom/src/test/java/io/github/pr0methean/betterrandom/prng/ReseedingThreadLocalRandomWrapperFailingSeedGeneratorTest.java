package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode;
import io.github.pr0methean.betterrandom.seed.FailingSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.io.Serializable;
import java.util.function.Supplier;
import org.testng.annotations.Test;

@Test(testName = "ReseedingThreadLocalRandomWrapper:FailingSeedGenerator")
public class ReseedingThreadLocalRandomWrapperFailingSeedGeneratorTest
    extends ReseedingThreadLocalRandomWrapperTest {

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
