package io.github.pr0methean.betterrandom.prng.adapter;

import static org.testng.Assert.assertEquals;

import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;

public class SplittableRandomAdapterTest extends SingleThreadSplittableRandomAdapterTest {

  @Override
  protected SplittableRandomAdapter tryCreateRng() throws SeedException {
    return new SplittableRandomAdapter(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  }

  @Override
  public void testSerializable() throws SeedException {
    final BaseSplittableRandomAdapter adapter = tryCreateRng();
    assertEquals(adapter, RandomTestUtils.serializeAndDeserialize(adapter));
  }

  @Override
  public void testSetSeed() {
    // No-op.
  }
  // TODO: Override or add tests for thread-safety.
}
