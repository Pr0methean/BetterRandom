package io.github.pr0methean.betterrandom.prng.concurrent;

import io.github.pr0methean.betterrandom.FlakyRetryAnalyzer;
import io.github.pr0methean.betterrandom.TestingDeficiency;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode;
import io.github.pr0methean.betterrandom.seed.DevRandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SemiFakeSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeeder;
import java.util.Random;
import java8.util.function.LongFunction;
import org.testng.annotations.Test;

@Test(testName = "ReseedingThreadLocalRandomWrapper:MersenneTwisterRandom")
public class ReseedingThreadLocalRandomWrapperMersenneTwisterTest
    extends ThreadLocalRandomWrapperMersenneTwisterTest {

  private final MersenneTwisterRandomColonColonNew mtSupplier
      = new MersenneTwisterRandomColonColonNew(getTestSeedGenerator());

  @TestingDeficiency @Override protected SeedGenerator getTestSeedGenerator() {
    // FIXME: Statistical tests often fail when using semiFakeSeedGenerator
    return SecureRandomSeedGenerator.DEFAULT_INSTANCE;
  }

  @Override public void testWrapLegacy() throws SeedException {
    ReseedingThreadLocalRandomWrapper.wrapLegacy(new LongFunction<Random>() {
      @Override public Random apply(long seed) {
        return new Random(seed);
      }
    }, getTestSeedGenerator()).nextInt();
  }

  @Override protected EntropyCheckMode getEntropyCheckMode() {
    return EntropyCheckMode.LOWER_BOUND;
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return ReseedingThreadLocalRandomWrapper.class;
  }

  @Override
  @Test(groups = "sequential", retryAnalyzer = FlakyRetryAnalyzer.class)
  public void testReseeding() {
    final SeedGenerator testSeedGenerator
        = new SemiFakeSeedGenerator(new SingleThreadSplittableRandomAdapter(), "testReseeding");
    final BaseRandom rng = new ReseedingThreadLocalRandomWrapper(testSeedGenerator, mtSupplier);
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
  public void testRandomSeederThreadIntegration() {
    createRng()
        .setRandomSeeder(new RandomSeederThread(DevRandomSeedGenerator.DEV_RANDOM_SEED_GENERATOR));
  }

  @Override @Test public void testSetSeedGeneratorNoOp() {
    SimpleRandomSeeder randomSeeder = new RandomSeederThread(getTestSeedGenerator());
    ReseedingThreadLocalRandomWrapper prng =
        new ReseedingThreadLocalRandomWrapper(mtSupplier, randomSeeder);
    prng.setRandomSeeder(randomSeeder);
  }

  @Override protected ReseedingThreadLocalRandomWrapper createRng() throws SeedException {
    return new ReseedingThreadLocalRandomWrapper(getTestSeedGenerator(), mtSupplier);
  }
}
