package io.github.pr0methean.betterrandom.prng;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import io.github.pr0methean.betterrandom.prng.adapter.SplittableRandomAdapter;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Supplier;
import org.testng.annotations.Test;

public class ThreadLocalRandomWrapperTest extends BaseRandomTest {

  private static BaseRandom createUnderlying() {
    try {
      return new AesCounterRandom();
    } catch (SeedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected Class<? extends BaseRandom> getClassUnderTest() {
    return ThreadLocalRandomWrapper.class;
  }

  @Override
  @Test(enabled = false)
  public void testAllPublicConstructors()
      throws SeedException, IllegalAccessException, InstantiationException, InvocationTargetException {
    // No-op: only 2 ctors, both tested elsewhere.
  }

  @Test
  public void testExplicitSeedSize() throws SeedException {
    assertEquals(new ThreadLocalRandomWrapper(200, DefaultSeedGenerator.DEFAULT_SEED_GENERATOR,
        AesCounterRandom::new).getNewSeedLength(), 200);
  }

  @Test
  public void testWrapLegacy() throws SeedException {
    ThreadLocalRandomWrapper.wrapLegacy(Random::new, DefaultSeedGenerator.DEFAULT_SEED_GENERATOR)
        .nextInt();
  }

  @Override
  protected BaseRandom tryCreateRng() throws SeedException {
    return new ThreadLocalRandomWrapper((Serializable & Supplier<BaseRandom>)
        ThreadLocalRandomWrapperTest::createUnderlying);
  }

  @Override
  protected BaseRandom createRng(byte[] seed) throws SeedException {
    return tryCreateRng();
  }

  /**
   * Since reseeding is thread-local, we can't use a {@link io.github.pr0methean.betterrandom.seed.RandomSeederThread}
   * for this test.
   */
  @Override
  public void testReseeding() throws SeedException {
    final byte[] output1 = new byte[20];
    final ThreadLocalRandomWrapper rng1 = (ThreadLocalRandomWrapper) createRng();
    final ThreadLocalRandomWrapper rng2 = (ThreadLocalRandomWrapper) createRng();
    rng1.nextBytes(output1);
    final byte[] output2 = new byte[20];
    rng2.nextBytes(output2);
    rng1.setSeed(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(8));
    rng1.nextBytes(output1);
    rng2.nextBytes(output2);
    assertFalse(Arrays.equals(output1, output2));
  }

}
