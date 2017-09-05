package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedException;

public class AesCounterRandom256Test extends AesCounterRandom128Test {

  @Override
  public BaseEntropyCountingRandom createRng() {
    try {
      return new AesCounterRandom(32);
    } catch (SeedException e) {
      throw new RuntimeException(e);
    }
  }
}
