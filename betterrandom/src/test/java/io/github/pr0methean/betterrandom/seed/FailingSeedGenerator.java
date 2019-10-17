package io.github.pr0methean.betterrandom.seed;

/**
 * A {@link SeedGenerator} that always throws a {@link SeedException} when asked to generate a seed.
 */
public class FailingSeedGenerator implements SeedGenerator {
  public static final FailingSeedGenerator DEFAULT_INSTANCE = new FailingSeedGenerator();
  private static final long serialVersionUID = 7201814102002000448L;

  @Override public void generateSeed(final byte[] output) throws SeedException {
    throw new SeedException("This is a FailingSeedGenerator");
  }

  @Override public boolean isWorthTrying() {
    return false;
  }

}
