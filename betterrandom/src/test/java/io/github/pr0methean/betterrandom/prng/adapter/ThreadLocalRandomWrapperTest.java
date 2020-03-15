package io.github.pr0methean.betterrandom.prng.adapter;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import com.google.common.testing.SerializableTester;
import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.BaseRandomTest;
import io.github.pr0methean.betterrandom.prng.Pcg64Random;
import io.github.pr0methean.betterrandom.seed.RandomSeeder;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import org.testng.annotations.Test;

public class ThreadLocalRandomWrapperTest<T extends BaseRandom>
    extends BaseRandomTest<ThreadLocalRandomWrapper<T>> {
  protected final Supplier<T> supplier;

  public ThreadLocalRandomWrapperTest(Supplier<T> supplier) {
    this.supplier = supplier;
  }

  @Override public void testSerializable() throws SeedException {
    // May change after serialization, so test only that it still works at all afterward
    SerializableTester.reserialize(createRng()).nextInt();
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

  @SuppressWarnings("rawtypes")
  @Override protected Class<? extends ThreadLocalRandomWrapper> getClassUnderTest() {
    return ThreadLocalRandomWrapper.class;
  }

  @Override @Test(enabled = false) public void testRepeatability() throws SeedException {
    // No-op: ThreadLocalRandomWrapper isn't repeatable.
  }

  @Override @Test(enabled = false) public void testRepeatabilityNextGaussian() {
    // No-op: ThreadLocalRandomWrapper isn't repeatable.
  }

  /**
   * setRandomSeeder doesn't work on this class and shouldn't pretend to.
   */
  @Override @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testRandomSeederIntegration() {
    createRng().setRandomSeeder(new RandomSeeder(getTestSeedGenerator()));
  }

  @Test public void testSetSeedGeneratorNoOp() {
    createRng().setRandomSeeder(null);
  }

  /**
   * Assertion-free because ThreadLocalRandomWrapper isn't repeatable.
   */
  @Override @Test public void testSetSeedAfterNextLong() throws SeedException {
    final byte[] seed = getTestSeedGenerator().generateSeed(getNewSeedLength());
    final BaseRandom rng = createRng();
    rng.nextLong();
    rng.setSeed(seed);
  }

  /**
   * Assertion-free because ThreadLocalRandomWrapper isn't repeatable.
   */
  @Override @Test public void testSetSeedAfterNextInt() throws SeedException {
    final byte[] seed = getTestSeedGenerator().generateSeed(getNewSeedLength());
    final BaseRandom rng = createRng();
    rng.nextInt();
    rng.setSeed(seed);
  }

  /**
   * Assertion-free because thread-local.
   */
  @Override @Test public void testThreadSafety() {
    checkThreadSafetyVsCrashesOnly(30, functionsForThreadSafetyTest);
  }

  @Override protected Map<Class<?>, Object> constructorParams() {
    final Map<Class<?>, Object> params = super.constructorParams();
    params.put(Supplier.class, supplier);
    params.put(Function.class, (Function<byte[], BaseRandom>) Pcg64Random::new);
    return params;
  }

  @Test public void testExplicitSeedSize() throws SeedException {
    assertEquals(new ThreadLocalRandomWrapper<>(200, getTestSeedGenerator(), AesCounterRandom::new)
        .getNewSeedLength(), 200);
  }

  @Test public void testWrapLegacy() throws SeedException {
    ThreadLocalRandomWrapper.wrapLegacy(Random::new, getTestSeedGenerator()).nextInt();
  }

  @Override protected ThreadLocalRandomWrapper<?> createRng() throws SeedException {
    return new ThreadLocalRandomWrapper(supplier);
  }

  @Override protected ThreadLocalRandomWrapper<?> createRng(final byte[] seed) throws SeedException {
    final ThreadLocalRandomWrapper<?> rng = createRng();
    rng.setSeed(seed);
    return rng;
  }

  @Test public void testGetWrapped() {
    assertSame(createRng().getWrapped().getClass(), Pcg64Random.class);
  }
}
