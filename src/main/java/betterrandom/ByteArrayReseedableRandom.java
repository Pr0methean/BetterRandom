package betterrandom;

import betterrandom.prng.BaseRandom;
import java.util.Random;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/**
 * A {@link java.util.Random} that can be reseeded using a byte array instead of using {@link
 * java.util.Random#setSeed(long)} (although that may also be supported).
 */
public interface ByteArrayReseedableRandom /* extends BaseRandom */ {

  /**
   * Reseed this PRNG. When Checker Framework is used, this can only non-nullably refer to a field
   * called "lock".
   */
  void setSeed(@UnknownInitialization(BaseRandom.class) byte[] seed);

  /**
   * Returns the supported length of a new byte-array seed, or the shortest optimal length if
   * multiple lengths are supported.
   */
  int getNewSeedLength();
}
