package io.github.pr0methean.betterrandom.prng.concurrent;

import io.github.pr0methean.betterrandom.FlakyRetryAnalyzer;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeederThread;
import java.io.Serializable;
import java.util.Random;
import java8.util.function.LongFunction;
import org.testng.annotations.Test;

@Test(testName = "ReseedingThreadLocalRandomWrapper")
public class ReseedingThreadLocalRandomWrapperTest extends ThreadLocalRandomWrapperTest {

  @Override public void testWrapLegacy() throws SeedException {
    ReseedingThreadLocalRandomWrapper
        .wrapLegacy(new RandomColonColonNewForLong(), getTestSeedGenerator()).nextInt();
  }

  @Override protected EntropyCheckMode getEntropyCheckMode() {
    return EntropyCheckMode.LOWER_BOUND;
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return ReseedingThreadLocalRandomWrapper.class;
  }

  /**
   * setRandomSeeder doesn't work on this class and shouldn't pretend to.
   */
  @Override @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testRandomSeederThreadIntegration() {
    createRng().setRandomSeeder(
        new RandomSeederThread(SecureRandomSeedGenerator.DEFAULT_INSTANCE));
  }

  @Test public void testSetSeedGeneratorNoOp() {
    SimpleRandomSeederThread randomSeeder = new RandomSeederThread(getTestSeedGenerator());
    ReseedingThreadLocalRandomWrapper prng =
        new ReseedingThreadLocalRandomWrapper(pcgSupplier, randomSeeder);
    prng.setRandomSeeder(randomSeeder);
  }

  @SuppressWarnings("BusyWait") @Override @Test(retryAnalyzer = FlakyRetryAnalyzer.class)
  public void testReseeding() {
    final SeedGenerator testSeedGenerator = getTestSeedGenerator();
    final BaseRandom rng = new ReseedingThreadLocalRandomWrapper(testSeedGenerator, pcgSupplier);
    RandomTestUtils.testReseeding(testSeedGenerator, rng, false);
  }

  /**
   * Assertion-free since reseeding may cause divergent output.
   */
  @Test(timeOut = 10000) public void testSetSeedLong() {
    createRng().setSeed(0x0123456789ABCDEFL);
  }

  /**
   * Test for crashes only, since setSeed is a no-op.
   */
  @Override @Test public void testSetSeedAfterNextLong() throws SeedException {
    final BaseRandom prng = createRng();
    prng.nextLong();
    prng.setSeed(getTestSeedGenerator().generateSeed(8));
    prng.nextLong();
  }

  /**
   * Test for crashes only, since setSeed is a no-op.
   */
  @Override @Test public void testSetSeedAfterNextInt() throws SeedException {
    final BaseRandom prng = createRng();
    prng.nextInt();
    prng.setSeed(getTestSeedGenerator().generateSeed(8));
    prng.nextInt();
  }

  @Override protected ReseedingThreadLocalRandomWrapper createRng() throws SeedException {
    return new ReseedingThreadLocalRandomWrapper(getTestSeedGenerator(), pcgSupplier);
  }

  private static class RandomColonColonNewForLong implements LongFunction<Random> {

    @Override public Random apply(long seed) {
      return new Random(seed);
    }
  }
}
