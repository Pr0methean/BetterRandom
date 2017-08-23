package betterrandom.prng.adapter;

import betterrandom.seed.SeedException;

public class ReseedingSplittableRandomAdapterTest extends SingleThreadSplittableRandomAdapterTest {
  @Override
  protected ReseedingSplittableRandomAdapter createAdapter() throws SeedException {
    return ReseedingSplittableRandomAdapter.getDefaultInstance();
  }
}
