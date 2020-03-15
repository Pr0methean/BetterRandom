package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.FlakyRetryAnalyzer;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.MersenneTwisterRandom;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode;
import io.github.pr0methean.betterrandom.seed.DevRandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeeder;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.util.Random;
import org.testng.annotations.Test;

@Test(testName = "ReseedingThreadLocalRandomWrapper:MersenneTwisterRandom")
public class ReseedingThreadLocalRandomWrapperMersenneTwisterTest
    extends ThreadLocalRandomWrapperMersenneTwisterTest {

  @Override public void testWrapLegacy() throws SeedException {
    ReseedingThreadLocalRandomWrapper.wrapLegacy(Random::new, getTestSeedGenerator()).nextInt();
  }

  @Override protected EntropyCheckMode getEntropyCheckMode() {
    return EntropyCheckMode.LOWER_BOUND;
  }

  @SuppressWarnings("rawtypes")
  @Override protected Class<ReseedingThreadLocalRandomWrapper> getClassUnderTest() {
    return ReseedingThreadLocalRandomWrapper.class;
  }

  @Override
  @Test(retryAnalyzer = FlakyRetryAnalyzer.class)
  public void testReseeding() {
    final SeedGenerator testSeedGenerator = getTestSeedGenerator();
    final BaseRandom rng = new ReseedingThreadLocalRandomWrapper<BaseRandom>(testSeedGenerator, supplier);
    RandomTestUtils.checkReseeding(testSeedGenerator, rng, false);
  }

  /**
   * Assertion-free since reseeding may cause divergent output.
   */
  @Override @Test public void testSetSeedLong() {
    createRng().setSeed(0x0123456789ABCDEFL);
  }

  /**
   * Test for crashes only, since setSeed is a no-op.
   */
  @Override @Test public void testSetSeedAfterNextLong() throws SeedException {
    final BaseRandom prng = createRng();
    prng.nextLong();
    prng.setSeed(getTestSeedGenerator().generateSeed(16));
    prng.nextLong();
  }

  /**
   * Test for crashes only, since setSeed is a no-op.
   */
  @Override @Test public void testSetSeedAfterNextInt() throws SeedException {
    final BaseRandom prng = createRng();
    prng.nextInt();
    prng.setSeed(getTestSeedGenerator().generateSeed(16));
    prng.nextInt();
  }

  /**
   * setRandomSeeder doesn't work on this class and shouldn't pretend to.
   */
  @Override @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testRandomSeederIntegration() {
    createRng()
        .setRandomSeeder(new RandomSeeder(DevRandomSeedGenerator.DEV_RANDOM_SEED_GENERATOR));
  }

  @Override @Test public void testSetSeedGeneratorNoOp() {
    RandomSeeder randomSeeder = new RandomSeeder(getTestSeedGenerator());
    ReseedingThreadLocalRandomWrapper<BaseRandom> prng =
        new ReseedingThreadLocalRandomWrapper<>(supplier, randomSeeder);
    prng.setRandomSeeder(randomSeeder);
  }

  @Override protected ReseedingThreadLocalRandomWrapper<MersenneTwisterRandom> createRng() throws SeedException {
    return new ReseedingThreadLocalRandomWrapper<>(getTestSeedGenerator(), supplier);
  }
}
