package io.github.pr0methean.betterrandom.seed;

import io.github.pr0methean.betterrandom.prng.concurrent.SplittableRandomAdapter;
import java.util.Random;

public class SemiFakeSeedGenerator extends FakeSeedGenerator {

  private static final long serialVersionUID = 3490669976564244209L;
  private final Random random;

  public SemiFakeSeedGenerator(final Random random) {
    this.random = random;
  }

  public SemiFakeSeedGenerator(Random random, String name) {
    super(name);
    this.random = random;
  }

  @Override public void generateSeed(final byte[] output) throws SeedException {
    random.nextBytes(output);
  }

  @Override public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SemiFakeSeedGenerator)) {
      return false;
    } else {
      SemiFakeSeedGenerator other = (SemiFakeSeedGenerator) o;
      return random.equals(other.random) && name.equals(other.name);
    }
  }

  @Override public int hashCode() {
    return random.hashCode();
  }
}
