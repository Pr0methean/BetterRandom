package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;

public class SplittableRandomAdapterTest extends SingleThreadSplittableRandomAdapterTest {

  @Override
  protected SplittableRandomAdapter tryCreateRng() throws SeedException {
    return new SplittableRandomAdapter(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  }

  /** This test only ensures that deserialization produces a usable instance. */
  @Override
  public void testSerializable() throws SeedException {
    RandomTestUtils.serializeAndDeserialize(tryCreateRng()).nextInt();
  }

  @Override
  public void testSetSeed() {
    // No-op.
  }
  // TODO: Override or add tests for seederThread-safety.
}
