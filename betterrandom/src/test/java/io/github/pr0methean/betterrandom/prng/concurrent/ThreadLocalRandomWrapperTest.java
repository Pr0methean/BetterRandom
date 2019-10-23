package io.github.pr0methean.betterrandom.prng.concurrent;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import com.google.common.testing.SerializableTester;
import io.github.pr0methean.betterrandom.prng.AbstractLargeSeedRandomTest;
import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.Pcg64Random;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.SerializableSupplier;
import java.util.Map;
import java.util.Random;
import java8.util.function.Function;
import java8.util.function.LongFunction;
import java8.util.function.Supplier;
import org.testng.annotations.Test;

@Test(testName = "ThreadLocalRandomWrapper") public class ThreadLocalRandomWrapperTest
    extends AbstractLargeSeedRandomTest {

  protected Supplier<BaseRandom> pcgSupplier
      = new Pcg64RandomColonColonNew(getTestSeedGenerator());

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

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
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
  public void testRandomSeederThreadIntegration() {
    createRng().setRandomSeeder(new RandomSeederThread(getTestSeedGenerator()));
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
    testThreadSafetyVsCrashesOnly(30, functionsForThreadSafetyTest);
  }

  @Override protected Map<Class<?>, Object> constructorParams() {
    final Map<Class<?>, Object> params = super.constructorParams();
    params.put(Supplier.class, pcgSupplier);
    params.put(Function.class, new Function<byte[], BaseRandom>() {
      @Override public BaseRandom apply(byte[] seed) {
        return new Pcg64Random(seed);
      }
    });
    return params;
  }

  @Test public void testExplicitSeedSize() throws SeedException {
    assertEquals(new ThreadLocalRandomWrapper(200, getTestSeedGenerator(),
        new Function<byte[], BaseRandom>() {
          @Override public BaseRandom apply(byte[] seed) {
            return new AesCounterRandom(seed);
          }
        }).getNewSeedLength(), 200);
  }

  @Test public void testWrapLegacy() throws SeedException {
    ThreadLocalRandomWrapper.wrapLegacy(new LongFunction<Random>() {
      @Override public Random apply(long seed) {
        return new Random(seed);
      }
    }, getTestSeedGenerator()).nextInt();
  }
  
  @Test public void testGetWrapped() {
    assertSame(createRng().getWrapped().getClass(), Pcg64Random.class);
  }

  @Override protected ThreadLocalRandomWrapper createRng() throws SeedException {
    return new ThreadLocalRandomWrapper(pcgSupplier);
  }

  @Override protected ThreadLocalRandomWrapper createRng(final byte[] seed) throws SeedException {
    final ThreadLocalRandomWrapper rng = createRng();
    rng.setSeed(seed);
    return rng;
  }

  protected static class Pcg64RandomColonColonNew
      implements SerializableSupplier<BaseRandom>, Function<byte[], BaseRandom> {

    private SeedGenerator seedGenerator;

    public Pcg64RandomColonColonNew(SeedGenerator seedGenerator) {
      this.seedGenerator = seedGenerator;
    }

    @Override public BaseRandom get() {
      return new Pcg64Random(seedGenerator);
    }

    @Override public BaseRandom apply(byte[] seed) {
      return new Pcg64Random(seed);
    }
  }
}
