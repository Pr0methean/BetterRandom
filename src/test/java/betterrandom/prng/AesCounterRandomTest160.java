package betterrandom.prng;

import betterrandom.seed.SeedException;

public class AesCounterRandomTest160 extends AesCounterRandomTest128 {
  @Override
  public BaseEntropyCountingRandom createRng() {
    try {
      return new AesCounterRandom(20);
    } catch (SeedException e) {
      throw new RuntimeException(e);
    }
  }
}
