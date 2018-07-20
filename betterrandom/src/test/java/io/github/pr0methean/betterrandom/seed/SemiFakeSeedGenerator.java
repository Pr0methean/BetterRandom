package io.github.pr0methean.betterrandom.seed;

import java.util.Objects;
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

  @Override
  public boolean equals(Object o) {
    return this == o
        || (o instanceof SemiFakeSeedGenerator
            && random.equals(o.random));
  }

  @Override
  public int hashCode() {
    return random.hashCode();
  }
}
