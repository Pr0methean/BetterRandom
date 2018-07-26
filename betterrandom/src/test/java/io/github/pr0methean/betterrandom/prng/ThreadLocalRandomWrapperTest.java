package io.github.pr0methean.betterrandom.prng;

import static org.testng.Assert.assertEquals;

import io.github.pr0methean.betterrandom.CloneViaSerialization;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.io.Serializable;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import org.testng.annotations.Test;

public class ThreadLocalRandomWrapperTest extends BaseRandomTest {

  private final Supplier<BaseRandom> pcgSupplier;

  public ThreadLocalRandomWrapperTest() {
    // Must be done first, or else lambda won't be serializable.
    final SeedGenerator seedGenerator = getTestSeedGenerator();

    pcgSupplier = (Supplier<BaseRandom> & Serializable)
        (() -> new Pcg64Random(seedGenerator));
  }

  @Override public void testSerializable()
      throws SeedException {
    // May change after serialization, so test only that it still works at all afterward
    CloneViaSerialization.clone(createRng()).nextInt();
  }

  @Override @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testSeedTooLong() throws SeedException {
    createRng().setSeed(getTestSeedGenerator().generateSeed(17));
  }

  @Override @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testSeedTooShort() throws SeedException {
    createRng().setSeed(new byte[]{1, 2, 3});
  }

  @Override @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testNullSeed() throws SeedException {
    createRng().setSeed(null);
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return ThreadLocalRandomWrapper.class;
  }

  @Override @Test(enabled = false) public void testRepeatability() throws SeedException {
    // No-op: ThreadLocalRandomWrapper isn't repeatable.
  }

  @Override @Test(enabled = false) public void testRepeatabilityNextGaussian() {
    // No-op: ThreadLocalRandomWrapper isn't repeatable.
  }

  /** Seeding of this PRNG is thread-local, so setSeederThread makes no sense. */
  @Override @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testRandomSeederThreadIntegration() {
    createRng().setSeedGenerator(getTestSeedGenerator());
  }

  /** Assertion-free because ThreadLocalRandomWrapper isn't repeatable. */
  @Override @Test public void testSetSeedAfterNextLong() throws SeedException {
    final byte[] seed =
        getTestSeedGenerator().generateSeed(getNewSeedLength(createRng()));
    final BaseRandom rng = createRng();
    rng.nextLong();
    rng.setSeed(seed);
  }

  /** Assertion-free because ThreadLocalRandomWrapper isn't repeatable. */
  @Override @Test public void testSetSeedAfterNextInt() throws SeedException {
    final byte[] seed =
        getTestSeedGenerator().generateSeed(getNewSeedLength(createRng()));
    final BaseRandom rng = createRng();
    rng.nextInt();
    rng.setSeed(seed);
  }


  /** Assertion-free because thread-local. */
  @Override @Test public void testThreadSafety() {
    testThreadSafetyVsCrashesOnly(FUNCTIONS_FOR_THREAD_SAFETY_TEST);
  }

  @Override public Map<Class<?>, Object> constructorParams() {
    final Map<Class<?>, Object> params = super.constructorParams();
    params.put(Supplier.class, pcgSupplier);
    params
        .put(Function.class, (Function<byte[], BaseRandom>) Pcg64Random::new);
    return params;
  }

  @Test public void testExplicitSeedSize() throws SeedException {
    assertEquals(new ThreadLocalRandomWrapper(200, getTestSeedGenerator(),
        AesCounterRandom::new).getNewSeedLength(), 200);
  }

  @Test public void testWrapLegacy() throws SeedException {
    ThreadLocalRandomWrapper.wrapLegacy(Random::new, getTestSeedGenerator()).nextInt();
  }

  @Override protected BaseRandom createRng() throws SeedException {
    return new ThreadLocalRandomWrapper(pcgSupplier);
  }

  @Override protected BaseRandom createRng(final byte[] seed) throws SeedException {
    BaseRandom rng = createRng();
    rng.setSeed(seed);
    return rng;
  }
}
