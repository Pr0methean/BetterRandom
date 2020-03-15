package io.github.pr0methean.betterrandom.seed;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class PseudorandomSeedGenerator extends FakeSeedGenerator {

  private static final long serialVersionUID = 3490669976564244209L;
  private final Random random;

  public PseudorandomSeedGenerator() {
    this(ThreadLocalRandom.current());
  }

  public PseudorandomSeedGenerator(final Random random) {
    this.random = random;
  }

  public PseudorandomSeedGenerator(Random random, String name) {
    super(name);
    this.random = random;
  }

  @Override public void generateSeed(final byte[] output) throws SeedException {
    statusChecks();
    random.nextBytes(output);
  }

  @Override public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PseudorandomSeedGenerator)) {
      return false;
    } else {
      PseudorandomSeedGenerator other = (PseudorandomSeedGenerator) o;
      return random.equals(other.random) && name.equals(other.name);
    }
  }

  @Override public int hashCode() {
    return random.hashCode();
  }
}
