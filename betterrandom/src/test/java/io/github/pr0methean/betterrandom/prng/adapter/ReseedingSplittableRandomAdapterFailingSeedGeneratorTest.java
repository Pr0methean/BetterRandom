package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode;
import io.github.pr0methean.betterrandom.seed.FailingSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;

public class ReseedingSplittableRandomAdapterFailingSeedGeneratorTest
    extends ReseedingSplittableRandomAdapterTest {

  @Override protected EntropyCheckMode getEntropyCheckMode() {
    return EntropyCheckMode.EXACT;
  }

  @Override public void testReseeding() {
    // No-op.
  }

  @Override protected ReseedingSplittableRandomAdapter createRng() throws SeedException {
    return ReseedingSplittableRandomAdapter.getInstance(FailingSeedGenerator.FAILING_SEED_GENERATOR);
  }
}
