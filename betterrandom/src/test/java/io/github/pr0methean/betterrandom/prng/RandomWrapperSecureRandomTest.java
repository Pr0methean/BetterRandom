package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.security.SecureRandom;
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
  public void testReseeding() throws Exception {
    // No-op.
  }

  private static final SecureRandom SEED_GEN = new SecureRandom();

  @Override
  protected BaseRandom tryCreateRng() throws SeedException {
    RandomWrapper wrapper = new RandomWrapper(new SecureRandom());
    wrapper.setSeed(SEED_GEN.nextLong());
    return wrapper;
  }

  @Override
  protected BaseRandom createRng(byte[] seed) throws SeedException {
    RandomWrapper wrapper = new RandomWrapper(new SecureRandom());
    wrapper.setSeed(seed);
    return wrapper;
  }
}
