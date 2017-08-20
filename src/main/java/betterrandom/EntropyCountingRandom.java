package betterrandom;

/**
 * A {@link java.util.Random} that can track its inflow and outflow of entropy (assuming that seeds
 * have entropy equal to their length) so we can decide whether or not it needs reseeding again
 * yet.
 */
public interface EntropyCountingRandom {

  /**
   * @return The current amount of entropy (bytes seeded with or bytes of internal state, whichever
   * was greater at the time, minus bytes returned).
   */
  int entropyOctets();
}
