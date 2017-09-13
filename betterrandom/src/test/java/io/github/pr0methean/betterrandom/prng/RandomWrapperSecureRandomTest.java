package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.prng.BaseRandom.ENTROPY_OF_DOUBLE;
import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.checkRangeAndEntropy;

import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.Failing;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

public class RandomWrapperSecureRandomTest extends BaseRandomTest {

  private static final SecureRandom SEED_GEN = new SecureRandom();

  @Failing // Entropy count currently incorrect
  @Override
  public void testNextGaussian() throws Exception {
    BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, 2 * ENTROPY_OF_DOUBLE,
        () -> prng.nextGaussian() + prng.nextGaussian(), -Double.MAX_VALUE, Double.MAX_VALUE, false);
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
