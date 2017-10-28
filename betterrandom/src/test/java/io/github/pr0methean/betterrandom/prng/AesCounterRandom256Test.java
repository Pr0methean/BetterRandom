package io.github.pr0methean.betterrandom.prng;

public class AesCounterRandom256Test extends AesCounterRandom128Test {

  @Override protected int getNewSeedLength(final BaseRandom basePrng) {
    return 32;
  }

  @Override protected BaseRandom createRng() {
    return new AesCounterRandom(32);
  }
}
