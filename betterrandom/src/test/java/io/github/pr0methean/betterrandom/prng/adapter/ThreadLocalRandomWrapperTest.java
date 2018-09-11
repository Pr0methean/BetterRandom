package io.github.pr0methean.betterrandom.prng.adapter;

import static org.testng.Assert.assertEquals;

import io.github.pr0methean.betterrandom.CloneViaSerialization;
import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.BaseRandomTest;
import io.github.pr0methean.betterrandom.prng.Pcg64Random;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.SerializableSupplier;
import java.io.Serializable;
import java.util.Map;
import java.util.Random;
import java8.util.function.Function;
import java8.util.function.LongFunction;
import java8.util.function.Supplier;
import org.testng.annotations.Test;

@Test(testName = "ThreadLocalRandomWrapper")
public class ThreadLocalRandomWrapperTest extends BaseRandomTest {

  protected Supplier<BaseRandom> pcgSupplier
      = new Pcg64RandomColonColonNew(getTestSeedGenerator());

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

  /** setSeedGenerator doesn't work on this class and shouldn't pretend to. */
  @Override @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testRandomSeederThreadIntegration() {
    createRng().setSeedGenerator(getTestSeedGenerator());
  }

  @Test public void testSetSeedGeneratorNoOp() {
    createRng().setSeedGenerator(null);
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
    testThreadSafetyVsCrashesOnly(30, functionsForThreadSafetyTest);
  }

  @Override public Map<Class<?>, Object> constructorParams() {
    final Map<Class<?>, Object> params = super.constructorParams();
    params.put(Supplier.class, pcgSupplier);
    params.put(Function.class, pcgSupplier);
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

  @Override protected BaseRandom createRng() throws SeedException {
    return new ThreadLocalRandomWrapper(pcgSupplier);
  }

  @Override protected BaseRandom createRng(final byte[] seed) throws SeedException {
    final BaseRandom rng = createRng();
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
