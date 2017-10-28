package io.github.pr0methean.betterrandom.prng;

public class AesCounterRandom160Test extends AesCounterRandom128Test {

  @Override protected int getNewSeedLength(final BaseRandom basePrng) {
    return 20;
  }

  @Override public BaseRandom createRng() {
    return new AesCounterRandom(20);
  }
}
