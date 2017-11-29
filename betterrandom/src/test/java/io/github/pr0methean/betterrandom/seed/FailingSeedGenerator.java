package io.github.pr0methean.betterrandom.seed;

/**
 * A {@link SeedGenerator} that always throws a {@link SeedException} when asked to generate a seed.
 */
public enum FailingSeedGenerator implements SeedGenerator {
  FAILING_SEED_GENERATOR;

  @Override public byte[] generateSeed(int length) throws SeedException {
    throw new SeedException("This is the FailingSeedGenerator");
  }

  @Override public void generateSeed(byte[] output) throws SeedException {
    throw new SeedException("This is the FailingSeedGenerator");
  }

  @Override public boolean isWorthTrying() {
    return false;
  }

}
