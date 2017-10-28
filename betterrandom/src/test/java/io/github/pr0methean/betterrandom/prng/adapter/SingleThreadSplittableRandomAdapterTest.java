package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.BaseRandomTest;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import org.testng.annotations.Test;

public class SingleThreadSplittableRandomAdapterTest extends BaseRandomTest {

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return SingleThreadSplittableRandomAdapter.class;
  }

  /**
   * {@inheritDoc} Overridden in subclasses, so that subclassing the test can test the subclasses.
   */
  @Override protected BaseSplittableRandomAdapter createRng() throws SeedException {
    return new SingleThreadSplittableRandomAdapter(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  }

  @Override protected BaseRandom createRng(final byte[] seed) throws SeedException {
    final BaseSplittableRandomAdapter adapter = createRng();
    adapter.setSeed(seed);
    return adapter;
  }

  @Test public void testGetSplittableRandom() throws Exception {
    // TODO
  }
}