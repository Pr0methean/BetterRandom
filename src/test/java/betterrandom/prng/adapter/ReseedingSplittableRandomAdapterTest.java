package betterrandom.prng.adapter;

import betterrandom.seed.SeedException;

public class ReseedingSplittableRandomAdapterTest extends SplittableRandomAdapterTest {
  @Override
  protected ReseedingSplittableRandomAdapter createAdapter() throws SeedException {
    return ReseedingSplittableRandomAdapter.getDefaultInstance();
  }
}
