package io.github.pr0methean.betterrandom;

/**
 * A {@link java.util.Random} that can track its inflow and outflow of entropy so we can determine
 * when it needs reseeding again.
 *
 * @author Chris Hennick
 */
public interface EntropyCountingRandom {

  /**
   * Returns an estimate of the current amount of entropy. Every time the PRNG is reseeded, the
   * entropy count is set to the new seed's length; and every time it is used, it is decreased by
   * the number of random bits in the output rounded up. The amount of entropy can go below zero,
   * giving an indication of how far the entropy has been stretched. This estimate is a lower bound
   * if the seed is perfectly random and is not being reused.
   *
   * @return The current estimated amount of entropy.
   */
  long getEntropyBits();

  /**
   * If true, this PRNG needs reseeding even though its entropy is positive. Added to deal with
   * {@link io.github.pr0methean.betterrandom.prng.concurrent.EntropyBlockingRandomWrapper}.
   *
   * @return true if this PRNG needs reseeding regardless of entropy count; false otherwise
   */
  boolean needsReseedingEarly();
}
