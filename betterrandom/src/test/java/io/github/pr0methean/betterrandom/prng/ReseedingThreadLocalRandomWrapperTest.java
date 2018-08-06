package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.TestUtils.assertGreaterOrEqual;

import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.io.Serializable;
import java.util.Random;
import java.util.function.Supplier;
import org.testng.annotations.Test;

@Test(testName = "ReseedingThreadLocalRandomWrapper")
public class ReseedingThreadLocalRandomWrapperTest extends ThreadLocalRandomWrapperTest {

  protected Supplier<? extends BaseRandom> pcgSupplier;

  public ReseedingThreadLocalRandomWrapperTest() {
    // Must be done first, or else lambda won't be serializable.
    SeedGenerator seedGenerator = getTestSeedGenerator();

    pcgSupplier = (Serializable & Supplier<BaseRandom>) () -> new Pcg64Random(seedGenerator);
  }

  @Override public void testWrapLegacy() throws SeedException {
    ReseedingThreadLocalRandomWrapper.wrapLegacy(Random::new, getTestSeedGenerator()).nextInt();
  }

  @Override protected EntropyCheckMode getEntropyCheckMode() {
    return EntropyCheckMode.LOWER_BOUND;
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return ReseedingThreadLocalRandomWrapper.class;
  }

  @SuppressWarnings("BusyWait") @Override @Test public void testReseeding() {
    final SeedGenerator testSeedGenerator = getTestSeedGenerator();
    final BaseRandom rng = new ReseedingThreadLocalRandomWrapper(testSeedGenerator,
        pcgSupplier);
    RandomTestUtils.testThreadLocalReseeding(testSeedGenerator, rng);
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
        (Serializable & Supplier<BaseRandom>) pcgSupplier);
  }
}
