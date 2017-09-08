package io.github.pr0methean.betterrandom.prng.adapter;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.SplittableRandom;

/**
 * <p>SingleThreadSplittableRandomAdapter class.</p>
 *
 * @author ubuntu
 * @version $Id: $Id
 */
public class SingleThreadSplittableRandomAdapter extends DirectSplittableRandomAdapter {

  private static final long serialVersionUID = -1125374167384636394L;
  private boolean deserializedAndNotUsedSince = false;

  /**
   * <p>Constructor for SingleThreadSplittableRandomAdapter.</p>
   *
   * @param seedGenerator a {@link io.github.pr0methean.betterrandom.seed.SeedGenerator} object.
   * @throws io.github.pr0methean.betterrandom.seed.SeedException if any.
   */
  public SingleThreadSplittableRandomAdapter(final SeedGenerator seedGenerator)
      throws SeedException {
    this(seedGenerator.generateSeed(SEED_LENGTH_BYTES));
  }

  /**
   * <p>Constructor for SingleThreadSplittableRandomAdapter.</p>
   *
   * @param seed an array of byte.
   */
  public SingleThreadSplittableRandomAdapter(final byte[] seed) {
    super(seed);
  }

  /** {@inheritDoc} */
  @Override
  protected ToStringHelper addSubSubclassFields(final ToStringHelper original) {
    return original.add("deserializedAndNotUsedSince", deserializedAndNotUsedSince);
  }

  /** {@inheritDoc} */
  @Override
  protected SplittableRandom getSplittableRandom() {
    deserializedAndNotUsedSince = false;
    return underlying;
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    setSeed(seed);
    if (!deserializedAndNotUsedSince) {
      underlying = underlying.split(); // Ensures we aren't rewinding
      deserializedAndNotUsedSince = true; // Ensures serializing and deserializing is idempotent
    }
  }
}
