package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedException;

public class AesCounterRandom160Test extends AesCounterRandom128Test {

  @Override
  public BaseEntropyCountingRandom createRng() {
    try {
      return new AesCounterRandom(20);
    } catch (final SeedException e) {
      throw new RuntimeException(e);
    }
  }
}
