package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedException;
import java.security.GeneralSecurityException;
import org.testng.annotations.Test;

public class AesCounterRandom256Test extends AesCounterRandom128Test {

  @Override
  protected int getNewSeedLength(BaseRandom basePrng) {
    return 32;
  }

  @Override
  @Test(enabled = false)
  public void testSeedTooLong() throws GeneralSecurityException, SeedException {
    // No-op: redundant to super.
  }

  @Override
  protected BaseRandom createRng() {
    try {
      return new AesCounterRandom(32);
    } catch (final SeedException e) {
      throw new RuntimeException(e);
    }
  }
}
