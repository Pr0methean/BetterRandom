package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class RandomWrapperSecureRandomTest extends BaseRandomTest {

  private static final SecureRandom SEED_GEN = new SecureRandom();

  private static RandomWrapper createRngInternal() {
    try {
      return new RandomWrapper(SecureRandom.getInstance("SHA1PRNG"));
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return RandomWrapper.class;
  }

  @Override protected BaseRandom createRng() throws SeedException {
    final RandomWrapper wrapper = createRngInternal();
    wrapper.setSeed(SEED_GEN.nextLong());
    return wrapper;
  }

  @Override protected BaseRandom createRng(final byte[] seed) throws SeedException {
    final RandomWrapper wrapper = createRngInternal();
    wrapper.setSeed(seed);
    return wrapper;
  }
}
