package betterrandom.prng;

import betterrandom.seed.SeedException;

public class AesCounterRandomTest256 extends AesCounterRandomTest128 {

  @Override
  public BaseEntropyCountingRandom createRng() {
    try {
      return new AesCounterRandom(32);
    } catch (SeedException e) {
      throw new RuntimeException(e);
    }
  }
}
