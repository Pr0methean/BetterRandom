package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.prng.BaseRandom.ENTROPY_OF_DOUBLE;
import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.checkRangeAndEntropy;

import io.github.pr0methean.betterrandom.Failing;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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
  @Failing
  public void testNextGaussian() {
    final BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, 2 * ENTROPY_OF_DOUBLE,
        () -> prng.nextGaussian() + prng.nextGaussian(), -Double.MAX_VALUE, Double.MAX_VALUE,
        EntropyCheckMode.OFF);
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
