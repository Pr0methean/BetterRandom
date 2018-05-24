package io.github.pr0methean.betterrandom.seed;

import java.util.Random;

public class SemiFakeSeedGenerator implements SeedGenerator {

  private static final long serialVersionUID = 3490669976564244209L;
  private final Random random;

  public SemiFakeSeedGenerator(Random random) {
    this.random = random;
  }

  @Override
  public void generateSeed(byte[] output) throws SeedException {
    random.nextBytes(output);
  }

  @Override public byte[] generateSeed(final int length) throws SeedException {
    if (length <= 0) {
      return EMPTY_SEED;
    }
    final byte[] output = new byte[length];
    generateSeed(output);
    return output;
  }

  @Override
  public boolean isWorthTrying() {
    return true;
  }
}
