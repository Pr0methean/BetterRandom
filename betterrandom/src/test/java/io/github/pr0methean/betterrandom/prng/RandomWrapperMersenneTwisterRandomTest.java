package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedException;

public class RandomWrapperMersenneTwisterRandomTest extends BaseRandomTest {

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return RandomWrapper.class;
  }

  @Override protected RandomWrapper createRng() throws SeedException {
    return new RandomWrapper(new MersenneTwisterRandom());
  }

  @Override protected RandomWrapper createRng(final byte[] seed) throws SeedException {
    return new RandomWrapper(new MersenneTwisterRandom(seed));
  }
}
