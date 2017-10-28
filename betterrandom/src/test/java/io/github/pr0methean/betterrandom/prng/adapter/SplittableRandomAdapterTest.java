package io.github.pr0methean.betterrandom.prng.adapter;

import static org.testng.Assert.assertFalse;

import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.FakeSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import org.testng.annotations.Test;

public class SplittableRandomAdapterTest extends SingleThreadSplittableRandomAdapterTest {

  private static final SeedGenerator FAKE_SEED_GENERATOR = new FakeSeedGenerator();

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return SplittableRandomAdapter.class;
  }

  @Override protected SplittableRandomAdapter createRng() throws SeedException {
    return new SplittableRandomAdapter(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  }

  /** Seeding of this PRNG is thread-local, so setSeederThread makes no sense. */
  @Override @Test(expectedExceptions = UnsupportedOperationException.class) public void testRandomSeederThreadIntegration() throws Exception {
    createRng().setSeederThread(RandomSeederThread.getInstance(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR));
  }

  // TODO: Override or add tests for thread-safety.
}
