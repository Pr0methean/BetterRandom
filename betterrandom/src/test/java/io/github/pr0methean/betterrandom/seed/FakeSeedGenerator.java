package io.github.pr0methean.betterrandom.seed;

public class FakeSeedGenerator implements SeedGenerator {

  private static final long serialVersionUID = 2310664903337315190L;
  private final String name;

  public FakeSeedGenerator() {
    this("FakeSeedGenerator");
  }

  /**
   * Creates a named instance.
   * @param name the name of this FakeSeedGenerator, returned by {@link #toString()}
   */
  public FakeSeedGenerator(final String name) {
    this.name = name;
  }

  @Override public void generateSeed(final byte[] output) throws SeedException {
    // No-op.
  }

  @Override
  public String toString() {
    return name;
  }

  @Override public byte[] generateSeed(final int length) throws SeedException {
    if (length <= 0) {
      return EMPTY_SEED;
    }
    final byte[] output = new byte[length];
    generateSeed(output);
    return output;
  }

  @Override public boolean isWorthTrying() {
    return true;
  }

  @Override
  public boolean equals(final Object o) {
    return this == o
        || (o instanceof FakeSeedGenerator
            && name.equals(((FakeSeedGenerator) o).name));
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
