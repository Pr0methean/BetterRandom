package io.github.pr0methean.betterrandom;

/**
 * A {@link java.util.Random} that can be reseeded using a byte array instead of using {@link
 * java.util.Random#setSeed(long)} (although that may also be supported).
 *
 * @author Chris Hennick
 */
public interface ByteArrayReseedableRandom /* extends BaseRandom */ {

  /**
   * Reseed this PRNG.
   *
   * @param seed The PRNG's new seed.
   */
  void setSeed(byte[] seed);

  /**
   * Returns the preferred length of a new byte-array seed. "Preferred" is implementation-defined
   * when multiple seed lengths are supported, but should probably usually mean the longest one,
   * since the longer the seed, the more random the output.
   *
   * @return The desired length of a new byte-array seed.
   */
  int getNewSeedLength();

  /**
   * Indicates whether {@link java.util.Random#setSeed(long)} is recommended over {@link
   * #setSeed(byte[])} when the seed is already in the form of a {@code long}.
   *
   * @return true if {@link java.util.Random#setSeed(long)} will tend to perform better than {@link
   *     #setSeed(byte[])}.
   */
  boolean preferSeedWithLong();
}
