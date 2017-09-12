package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

public class RandomWrapperSecureRandomTest extends BaseRandomTest {

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
    BaseRandom prng = createRng();
    prng.nextLong();
    prng.setSeed(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(8));
    prng.nextLong();
  }

  @Override
  @Test(enabled = false)
  public void testRepeatability() throws SeedException {
    // No-op.
  }

  private static final SecureRandom SEED_GEN = new SecureRandom();

  @Override
  protected BaseRandom tryCreateRng() throws SeedException {
    RandomWrapper wrapper = createRngInternal();
    wrapper.setSeed(SEED_GEN.nextLong());
    return wrapper;
  }

  @NotNull
  private RandomWrapper createRngInternal() {
    RandomWrapper wrapper = null;
    try {
      wrapper = new RandomWrapper(SecureRandom.getInstance("SHA1PRNG"));
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    return wrapper;
  }

  @Override
  protected BaseRandom createRng(byte[] seed) throws SeedException {
    RandomWrapper wrapper = createRngInternal();
    wrapper.setSeed(seed);
    return wrapper;
  }
}
