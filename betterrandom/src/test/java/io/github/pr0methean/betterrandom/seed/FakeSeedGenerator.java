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

  /**
   * Generates and returns a seed value for a random number generator as a new array.
   * @param length The length of the seed to generate (in bytes).
   * @return A byte array containing the seed data.
   * @throws SeedException If a seed cannot be generated for any reason.
   */
  @Override public byte[] generateSeed(final int length) throws SeedException {
    if (length <= 0) {
      return EMPTY_SEED;
    }
    final byte[] output = new byte[length];
    generateSeed(output);
    return output;
  }

  /**
   * Returns true if we cannot determine quickly (i.e. without I/O calls) that this SeedGenerator
   * would throw a {@link SeedException} if {@link #generateSeed(int)} or {@link
   * #generateSeed(byte[])} were being called right now.
   * @return true if this SeedGenerator will get as far as an I/O call or other slow operation in
   *     attempting to generate a seed immediately.
   */
  @Override public boolean isWorthTrying() {
    return true;
  }

  @Override
  public boolean equals(Object o) {
    return this == o
        || (o instanceof FakeSeedGenerator
            && name.equals(o.name));
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
