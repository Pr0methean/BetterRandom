package io.github.pr0methean.betterrandom.prng.concurrent;

import static org.testng.Assert.assertEquals;

import io.github.pr0methean.betterrandom.TestingDeficiency;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import org.testng.annotations.Test;

public class SplittableRandomAdapterTest extends SingleThreadSplittableRandomAdapterTest {

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return SplittableRandomAdapter.class;
  }

  /**
   * SplittableRandomAdapter isn't repeatable until its seed has been specified.
   */
  @Override public void testRepeatability() throws SeedException {
    final BaseRandom rng = createRng();
    rng.setSeed(TEST_SEED);
    // Create second RNG using same seed.
    final BaseRandom duplicateRNG = createRng();
    duplicateRNG.setSeed(TEST_SEED);
    RandomTestUtils.assertEquivalent(rng, duplicateRNG, 1000, "Generated sequences do not match");
  }

  /**
   * SplittableRandomAdapter isn't repeatable until its seed has been specified.
   */
  @TestingDeficiency // Failing
  @Override @Test(enabled = false) public void testRepeatabilityNextGaussian()
      throws SeedException {
    final BaseRandom rng = createRng();
    final byte[] seed = getTestSeedGenerator().generateSeed(getNewSeedLength());
    rng.nextGaussian();
    rng.setSeed(seed);
    // Create second RNG using same seed.
    final BaseRandom duplicateRNG = createRng();
    duplicateRNG.setSeed(seed);
    assertEquals(rng.nextGaussian(), duplicateRNG.nextGaussian());
  }

  @Override protected SplittableRandomAdapter createRng() throws SeedException {
    return new SplittableRandomAdapter(getTestSeedGenerator());
  }

  /**
   * Seeding of this PRNG is thread-local, so setSeederThread makes no sense.
   */
  @Override @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testRandomSeederThreadIntegration() {
    createRng().setRandomSeeder(new RandomSeederThread(getTestSeedGenerator()));
  }

  @Test public void testSetSeedGeneratorNoOp() {
    createRng().setRandomSeeder(null);
  }

  /**
   * Assertion-free because thread-local.
   */
  @Override @Test public void testThreadSafety() {
    testThreadSafetyVsCrashesOnly(30, functionsForThreadSafetyTest);
  }
}
