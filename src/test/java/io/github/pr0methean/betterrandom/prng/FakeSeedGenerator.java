package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;

public class FakeSeedGenerator implements SeedGenerator {

  @Override
  public byte[] generateSeed(int length) throws SeedException {
    return new byte[length];
  }
}
