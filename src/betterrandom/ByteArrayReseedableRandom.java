package betterrandom;

/**
 * A {@link java.util.Random} that can be reseeded using a byte array instead of
 * using {@link java.util.Random#setSeed(long)} (although that may also be supported).
 */
public interface ByteArrayReseedableRandom {
  /** Reseed this PRNG. */
  public void setSeed(byte[] seed);

  /**
   * Returns the supported length of a new byte-array seed, or the shortest optimal length if
   * multiple lengths are supported.
   */
  public int getNewSeedLength();
}
