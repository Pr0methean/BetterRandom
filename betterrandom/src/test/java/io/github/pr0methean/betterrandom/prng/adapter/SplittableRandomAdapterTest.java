package io.github.pr0methean.betterrandom.prng.adapter;

import static org.testng.Assert.assertEquals;

import io.github.pr0methean.betterrandom.TestingDeficiency;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import io.github.pr0methean.betterrandom.seed.SeedException;
import org.testng.annotations.Test;

public class SplittableRandomAdapterTest extends SingleThreadSplittableRandomAdapterTest {

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return SplittableRandomAdapter.class;
  }

  /** SplittableRandomAdapter isn't repeatable until its seed has been specified. */
  @Override public void testRepeatability() throws SeedException {
    final BaseRandom rng = createRng();
    rng.setSeed(TEST_SEED);
    // Create second RNG using same seed.
    final BaseRandom duplicateRNG = createRng();
    duplicateRNG.setSeed(TEST_SEED);
    RandomTestUtils.assertEquivalent(rng, duplicateRNG, 1000, "Generated sequences do not match");
  }

  /** SplittableRandomAdapter isn't repeatable until its seed has been specified. */
  @TestingDeficiency // Failing
  @Override @Test(enabled = false)
  public void testRepeatabilityNextGaussian() throws SeedException {
    final BaseRandom rng = createRng();
    byte[] seed = SEMIFAKE_SEED_GENERATOR.generateSeed(getNewSeedLength(rng));
    rng.nextGaussian();
    rng.setSeed(seed);
    // Create second RNG using same seed.
    final BaseRandom duplicateRNG = createRng();
    duplicateRNG.setSeed(seed);
    assertEquals(rng.nextGaussian(), duplicateRNG.nextGaussian());
  }

  @Override protected SplittableRandomAdapter createRng() throws SeedException {
    return new SplittableRandomAdapter(SEMIFAKE_SEED_GENERATOR);
  }

  /** Seeding of this PRNG is thread-local, so setSeederThread makes no sense. */
  @Override @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testRandomSeederThreadIntegration() throws Exception {
    createRng().setSeedGenerator(SEMIFAKE_SEED_GENERATOR);
  }

  /** Assertion-free because thread-local. */
  @Override @Test public void testThreadSafety() {
    testThreadSafetyVsCrashesOnly(FUNCTIONS_FOR_THREAD_SAFETY_TEST);
  }
}
