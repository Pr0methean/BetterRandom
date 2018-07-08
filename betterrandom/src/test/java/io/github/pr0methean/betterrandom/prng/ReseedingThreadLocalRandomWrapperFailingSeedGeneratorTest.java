package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode;
import io.github.pr0methean.betterrandom.seed.FailingSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import org.testng.annotations.Test;

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

  @Override protected BaseRandom createRng() throws SeedException {
    return new ReseedingThreadLocalRandomWrapper(FailingSeedGenerator.FAILING_SEED_GENERATOR,
        new Pcg64RandomColonColonNew());
  }
}
