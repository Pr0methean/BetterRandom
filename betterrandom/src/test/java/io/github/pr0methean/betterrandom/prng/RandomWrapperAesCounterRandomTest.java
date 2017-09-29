package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedException;

public class RandomWrapperAesCounterRandomTest extends AesCounterRandom128Test {

  @Override
  protected RandomWrapper tryCreateRng() throws SeedException {
    return new RandomWrapper(new AesCounterRandom());
  }

  @Override
  protected RandomWrapper createRng(byte[] seed) throws SeedException {
    return new RandomWrapper(new AesCounterRandom(seed));
  }
}
