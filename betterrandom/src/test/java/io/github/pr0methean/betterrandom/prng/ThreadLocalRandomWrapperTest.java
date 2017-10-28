package io.github.pr0methean.betterrandom.prng;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.io.Serializable;
import java.util.Random;
import java.util.function.Supplier;
import org.testng.annotations.Test;

public class ThreadLocalRandomWrapperTest extends BaseRandomTest {

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return ThreadLocalRandomWrapper.class;
  }

  /** Seeding of this PRNG is thread-local, so setSeederThread makes no sense. */
  @Override @Test(expectedExceptions = UnsupportedOperationException.class) public void testRandomSeederThreadIntegration() throws Exception {
    createRng().setSeederThread(RandomSeederThread.getInstance(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR));
  }

  @Test public void testExplicitSeedSize() throws SeedException {
    assertEquals(new ThreadLocalRandomWrapper(200, DefaultSeedGenerator.DEFAULT_SEED_GENERATOR,
        AesCounterRandom::new).getNewSeedLength(), 200);
  }

  @Test public void testWrapLegacy() throws SeedException {
    ThreadLocalRandomWrapper.wrapLegacy(Random::new, DefaultSeedGenerator.DEFAULT_SEED_GENERATOR)
        .nextInt();
  }

  @Override protected BaseRandom createRng() throws SeedException {
    return new ThreadLocalRandomWrapper(
        (Serializable & Supplier<BaseRandom>) MersenneTwisterRandom::new);
  }

  @Override protected BaseRandom createRng(final byte[] seed) throws SeedException {
    return createRng();
  }
}
