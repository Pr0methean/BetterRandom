package io.github.pr0methean.betterrandom.prng.adapter;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.FakeSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import org.testng.annotations.Test;

public class ReseedingSplittableRandomAdapterTest extends SingleThreadSplittableRandomAdapterTest {

  @Override protected ReseedingSplittableRandomAdapter createRng() throws SeedException {
    return ReseedingSplittableRandomAdapter.getDefaultInstance();
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return ReseedingSplittableRandomAdapter.class;
  }

  @Override @Test(enabled = false) public void testReseeding() {
    // No-op.
  }

  /** This class manages its own interaction with a RandomSeederThread, so setSeederThread makes no sense. */
  @Override @Test(expectedExceptions = UnsupportedOperationException.class) public void testRandomSeederThreadIntegration() throws Exception {
    createRng().setSeederThread(RandomSeederThread.getInstance(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR));
  }

  @Test public void testFinalize() throws SeedException {
    ReseedingSplittableRandomAdapter.getInstance(new FakeSeedGenerator());
    Runtime.getRuntime().runFinalization();
  }
}
