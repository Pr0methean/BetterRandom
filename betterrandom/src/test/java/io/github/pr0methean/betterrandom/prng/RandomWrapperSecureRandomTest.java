package io.github.pr0methean.betterrandom.prng;

import com.google.common.collect.ImmutableMap;
import io.github.pr0methean.betterrandom.TestUtils;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

public class RandomWrapperSecureRandomTest extends BaseRandomTest {

  private static final SecureRandom SEED_GEN = new SecureRandom();

  @NotNull
  private static RandomWrapper createRngInternal() {
    try {
      return new RandomWrapper(SecureRandom.getInstance("SHA1PRNG"));
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected Class<? extends BaseRandom> getClassUnderTest() {
    return RandomWrapper.class;
  }

  @Override
  public void testAllPublicConstructors()
      throws SeedException, IllegalAccessException, InstantiationException, InvocationTargetException {
    BaseRandom basePrng = createRng();
    int seedLength = getNewSeedLength(basePrng);
    TestUtils.testAllPublicConstructors(getClassUnderTest(), ImmutableMap.of(
        int.class, seedLength,
        long.class, TEST_SEED,
        byte[].class, DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(seedLength),
        SeedGenerator.class, DefaultSeedGenerator.DEFAULT_SEED_GENERATOR,
        Random.class, new SecureRandom()
    ), BaseRandom::nextInt);
  }

  /**
   * {@link SecureRandom#setSeed(byte[])} has no length restriction, so disinherit {@link
   * Test#expectedExceptions()}.
   */
  @Override
  @Test
  public void testSeedTooLong() throws GeneralSecurityException, SeedException {
    super.testSeedTooLong();
  }

  /**
   * {@link SecureRandom#setSeed(byte[])} has no length restriction, so disinherit {@link
   * Test#expectedExceptions()}.
   */
  @Override
  @Test
  public void testSeedTooShort() throws SeedException {
    super.testSeedTooShort();
  }

  @Override
  @Test(enabled = false)
  public void testNullSeed() throws SeedException {
    // No-op.
  }

  /**
   * Only test for crashes, since {@link SecureRandom#setSeed(long)} doesn't completely replace the
   * existing seed.
   */
  @Override
  public void testSetSeed() throws SeedException {
    final BaseRandom prng = createRng();
    prng.nextLong();
    prng.setSeed(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(8));
    prng.nextLong();
  }

  @Override
  @Test(enabled = false)
  public void testRepeatability() throws SeedException {
    // No-op.
  }

  @Override
  protected BaseRandom tryCreateRng() throws SeedException {
    final RandomWrapper wrapper = createRngInternal();
    wrapper.setSeed(SEED_GEN.nextLong());
    return wrapper;
  }

  @Override
  protected BaseRandom createRng(final byte[] seed) throws SeedException {
    final RandomWrapper wrapper = createRngInternal();
    wrapper.setSeed(seed);
    return wrapper;
  }
}
