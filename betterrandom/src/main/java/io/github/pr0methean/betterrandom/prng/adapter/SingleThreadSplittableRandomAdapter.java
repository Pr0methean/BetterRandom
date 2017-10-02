package io.github.pr0methean.betterrandom.prng.adapter;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.SplittableRandom;

/**
 * Simple, non-thread-safe implementation of {@link io.github.pr0methean.betterrandom.prng.BaseRandom}
 * that wraps a {@link SplittableRandom}.
 *
 * @author Chris Hennick
 */
public class SingleThreadSplittableRandomAdapter extends DirectSplittableRandomAdapter {

  private static final long serialVersionUID = -1125374167384636394L;
  private boolean deserializedAndNotUsedSince = false;

  /**
   * Use the provided seed generation strategy to create the seed for the underlying {@link
   * SplittableRandom}.
   *
   * @param seedGenerator The seed generation strategy that will provide the seed value for this
   *     RNG.
   * @throws SeedException if there is a problem generating a seed.
   */
  public SingleThreadSplittableRandomAdapter(final SeedGenerator seedGenerator)
      throws SeedException {
    this(seedGenerator.generateSeed(Long.BYTES));
  }

  /**
   * Use the provided seed for the underlying {@link SplittableRandom}.
   *
   * @param seed The seed. Must be 8 bytes.
   */
  public SingleThreadSplittableRandomAdapter(final byte[] seed) {
    super(seed);
  }

  /**
   * Use the provided seed for the underlying {@link SplittableRandom}.
   *
   * @param seed The seed.
   */
  public SingleThreadSplittableRandomAdapter(final long seed) {
    super(seed);
  }

  @Override
  protected boolean useParallelStreams() {
    return false;
  }

  @Override
  protected ToStringHelper addSubSubclassFields(final ToStringHelper original) {
    return super.addSubSubclassFields(original)
        .add("deserializedAndNotUsedSince", deserializedAndNotUsedSince);
  }

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
