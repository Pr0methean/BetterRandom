package betterrandom.prng.adapter;

import betterrandom.seed.DefaultSeedGenerator;
import betterrandom.seed.SeedException;

public class SplittableRandomAdapterTest extends SingleThreadSplittableRandomAdapterTest {

  @Override
  protected SplittableRandomAdapter createAdapter() throws SeedException {
    return new SplittableRandomAdapter(DefaultSeedGenerator.INSTANCE);
  }

  // TODO: Override or add tests for thread-safety.
}
