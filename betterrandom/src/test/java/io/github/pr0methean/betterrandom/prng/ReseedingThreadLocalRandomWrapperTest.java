package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.TestUtils.assertGreaterOrEqual;
import static io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator.DEFAULT_SEED_GENERATOR;

import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Arrays;
import java.util.Random;
import java8.util.function.LongFunction;
import org.testng.annotations.Test;

public class ReseedingThreadLocalRandomWrapperTest extends ThreadLocalRandomWrapperTest {

  @Override public void testWrapLegacy() throws SeedException {
    ReseedingThreadLocalRandomWrapper
        .wrapLegacy(new RandomColonColonNewForLong(), DEFAULT_SEED_GENERATOR).nextInt();
  }

  @Override protected EntropyCheckMode getEntropyCheckMode() {
    return EntropyCheckMode.LOWER_BOUND;
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return ReseedingThreadLocalRandomWrapper.class;
  }

  @Override @Test public void testReseeding() {
    final BaseRandom rng = new ReseedingThreadLocalRandomWrapper(DEFAULT_SEED_GENERATOR,
        new MersenneTwisterRandomColonColonNew());
    rng.nextLong();
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    final byte[] oldSeed = rng.getSeed();
    try {
      byte[] newSeed;
      do {
        rng.nextLong();
        Thread.sleep(10);
        newSeed = rng.getSeed();
      } while (Arrays.equals(newSeed, oldSeed));
      Thread.sleep(10);
      assertGreaterOrEqual(rng.getEntropyBits(), (newSeed.length * 8L) - 1);
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /** Assertion-free since reseeding may cause divergent output. */
  @Test(timeOut = 10000) public void testSetSeedLong() {
    createRng().setSeed(0x0123456789ABCDEFL);
  }

  /** Test for crashes only, since setSeed is a no-op. */
  @Override @Test public void testSetSeedAfterNextLong() throws SeedException {
    final BaseRandom prng = createRng();
    prng.nextLong();
    prng.setSeed(DEFAULT_SEED_GENERATOR.generateSeed(16));
    prng.nextLong();
  }

  /** Test for crashes only, since setSeed is a no-op. */
  @Override @Test public void testSetSeedAfterNextInt() throws SeedException {
    final BaseRandom prng = createRng();
    prng.nextInt();
    prng.setSeed(DEFAULT_SEED_GENERATOR.generateSeed(16));
    prng.nextInt();
  }

  @Override protected BaseRandom createRng() throws SeedException {
    return new ReseedingThreadLocalRandomWrapper(DEFAULT_SEED_GENERATOR,
        new MersenneTwisterRandomColonColonNew());
  }

  private static class RandomColonColonNewForLong implements LongFunction<Random> {

    @Override public Random apply(long seed) {
      return new Random(seed);
    }
  }
}
