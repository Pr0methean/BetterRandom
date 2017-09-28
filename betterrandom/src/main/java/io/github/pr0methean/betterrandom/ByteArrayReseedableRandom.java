package io.github.pr0methean.betterrandom;

/**
 * A {@link java.util.Random} that can be reseeded using a byte array instead of using {@link
 * java.util.Random#setSeed(long)} (although that may also be supported).
 *
 * @author Chris Hennick
 * @version $Id: $Id
 */
public interface ByteArrayReseedableRandom /* extends BaseRandom */ {

  /**
   * Reseed this PRNG.
   *
   * @param seed The PRNG's new seed.
   */
  void setSeed(byte[] seed);

  /**
   * @return The supported length of a new byte-array seed, or the optimal length if multiple
   *     lengths are supported.
   */
  int getNewSeedLength();

  /**
   * @return true if {@link java.util.Random#setSeed(long)} will tend to perform better than {@link
   *     #setSeed(byte[])}.
   */
  default boolean preferSeedWithLong() {
    return false;
  }
}
