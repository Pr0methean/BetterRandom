package io.github.pr0methean.betterrandom.prng;

public class ChaCha20CounterRandomTest extends CipherCounterRandomTest {
  @Override
  protected Class<? extends BaseRandom> getClassUnderTest() {
    return ChaCha20CounterRandom.class;
  }

  @Override
  protected int getExpectedMaxSize() {
    return 96;
  }

  @Override
  protected BaseRandom createRng() {
    return new ChaCha20CounterRandom(getTestSeedGenerator().generateSeed(seedSizeBytes));
  }

  @Override
  protected BaseRandom createRng(byte[] seed) {
    return new ChaCha20CounterRandom(seed);
  }
}
