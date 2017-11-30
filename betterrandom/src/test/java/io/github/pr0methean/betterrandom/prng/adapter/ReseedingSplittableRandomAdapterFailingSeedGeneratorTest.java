package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode;
import io.github.pr0methean.betterrandom.seed.FailingSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import org.testng.annotations.Test;

public class ReseedingSplittableRandomAdapterFailingSeedGeneratorTest
    extends ReseedingSplittableRandomAdapterTest {

  @Override protected EntropyCheckMode getEntropyCheckMode() {
    return EntropyCheckMode.EXACT;
  }

  @Override @Test(enabled = false) public void testReseeding() {
    // No-op.
  }

  @Override protected ReseedingSplittableRandomAdapter createRng() throws SeedException {
    return ReseedingSplittableRandomAdapter.getInstance(FailingSeedGenerator.FAILING_SEED_GENERATOR);
  }
}
