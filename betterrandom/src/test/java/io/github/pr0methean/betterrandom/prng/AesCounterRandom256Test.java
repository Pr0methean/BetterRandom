package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedException;

public class AesCounterRandom256Test extends AesCounterRandom128Test {

  @Override
  protected int getNewSeedLength(BaseRandom basePrng) {
    return 32;
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
