package io.github.pr0methean.betterrandom.prng;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.CloneViaSerialization;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Supplier;
import org.testng.annotations.Test;

public class ThreadLocalRandomWrapperTest extends BaseRandomTest {

  @Override
  public void testSerializable() throws IOException, ClassNotFoundException, SeedException {
    // May change after serialization, so test only that it still works at all afterward
    CloneViaSerialization.clone(createRng()).nextInt();
  }

  @Override
  @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testSeedTooLong() throws GeneralSecurityException, SeedException {
    createRng().setSeed(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(17));
  }

  @Override
  @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testSeedTooShort() throws SeedException {
    createRng().setSeed(new byte[]{1, 2, 3});
  }

  @Override
  @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testNullSeed() throws SeedException {
    createRng().setSeed(null);
  }

  @Override
  protected Class<? extends BaseRandom> getClassUnderTest() {
    return ThreadLocalRandomWrapper.class;
  }

  @Override
  @Test(enabled = false)
  public void testRepeatability() throws SeedException {
    // No-op: ThreadLocalRandomWrapper isn't repeatable.
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
  protected BaseRandom createRng() throws SeedException {
    return new ThreadLocalRandomWrapper((Serializable & Supplier<BaseRandom>)
        MersenneTwisterRandom::new);
  }

  @Override
  protected BaseRandom createRng(final byte[] seed) throws SeedException {
    return createRng();
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
    rng1.setSeed(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(16));
    rng1.nextBytes(output1);
    rng2.nextBytes(output2);
    assertFalse(Arrays.equals(output1, output2));
  }
}
