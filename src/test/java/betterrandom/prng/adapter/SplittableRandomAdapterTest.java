package betterrandom.prng.adapter;

import betterrandom.seed.DefaultSeedGenerator;
import betterrandom.seed.SeedException;

public class SplittableRandomAdapterTest extends SingleThreadSplittableRandomAdapterTest {

  @Override
  protected SplittableRandomAdapter tryCreateRng() throws SeedException {
    return new SplittableRandomAdapter(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  }

  @Override
  public void testSetSeed() {
    // No-op.
  }
  // TODO: Override or add tests for thread-safety.
}
