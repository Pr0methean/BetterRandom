package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode;
import io.github.pr0methean.betterrandom.seed.FailingSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.io.Serializable;
import java.util.function.Supplier;
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
        (Serializable & Supplier<BaseRandom>) Pcg64Random::new);
  }
}
