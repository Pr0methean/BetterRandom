package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.TestUtils.assertGreaterOrEqual;
import static io.github.pr0methean.betterrandom.TestUtils.isAppveyor;

import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;
import java8.util.function.LongFunction;
import org.testng.SkipException;
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

  @SuppressWarnings("BusyWait") @Override @Test public void testReseeding() {
    final BaseRandom rng = new ReseedingThreadLocalRandomWrapper(getTestSeedGenerator(),
        pcgSupplier);
    rng.nextLong();
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    final byte[] oldSeed = rng.getSeed();
    byte[] newSeed;
    RandomSeederThread.setPriority(getTestSeedGenerator(), Thread.MAX_PRIORITY);
    try {
      do {
        rng.nextLong();
        Thread.sleep(50);
        newSeed = rng.getSeed();
      } while (Arrays.equals(newSeed, oldSeed));
      Thread.sleep(200);
      assertGreaterOrEqual(rng.getEntropyBits(), (newSeed.length * 8L) - 1);
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      RandomSeederThread.setPriority(getTestSeedGenerator(), Thread.NORM_PRIORITY);
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
    prng.setSeed(getTestSeedGenerator().generateSeed(8));
    prng.nextLong();
  }

  /** Test for crashes only, since setSeed is a no-op. */
  @Override @Test public void testSetSeedAfterNextInt() throws SeedException {
    final BaseRandom prng = createRng();
    prng.nextInt();
    prng.setSeed(getTestSeedGenerator().generateSeed(8));
    prng.nextInt();
  }

  @Override protected BaseRandom createRng() throws SeedException {
    return new ReseedingThreadLocalRandomWrapper(getTestSeedGenerator(),
        pcgSupplier);
  }

  private static class RandomColonColonNewForLong implements LongFunction<Random> {

    @Override public Random apply(long seed) {
      return new Random(seed);
    }
  }
}
