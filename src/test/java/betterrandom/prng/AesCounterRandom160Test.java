package betterrandom.prng;

import betterrandom.seed.SeedException;

public class AesCounterRandom160Test extends AesCounterRandom128Test {

  @Override
  public BaseEntropyCountingRandom createRng() {
    try {
      return new AesCounterRandom(20);
    } catch (SeedException e) {
      throw new RuntimeException(e);
    }
  }
}
