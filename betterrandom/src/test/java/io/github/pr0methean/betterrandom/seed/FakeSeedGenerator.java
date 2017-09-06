package io.github.pr0methean.betterrandom.seed;

public class FakeSeedGenerator implements SeedGenerator {

  @Override
  public byte[] generateSeed(int length) throws SeedException {
    return new byte[length];
  }
}
