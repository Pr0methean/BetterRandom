package io.github.pr0methean.betterrandom.prng.concurrent;

import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.SplittableRandom;

/**
 * Simple, non-thread-safe implementation of
 * {@link io.github.pr0methean.betterrandom.prng.BaseRandom}
 * that wraps a {@link SplittableRandom}.
 *
 * @author Chris Hennick
 */
public class SingleThreadSplittableRandomAdapter extends DirectSplittableRandomAdapter {

  private static final long serialVersionUID = -1125374167384636394L;

  /**
   * Use the provided seed generation strategy to create the seed for the {@link SplittableRandom}.
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
   * Use the {@link DefaultSeedGenerator} to create the seed for the {@link SplittableRandom}.
   *
   * @throws SeedException if the {@link DefaultSeedGenerator} fails to generate a seed.
   */
  public SingleThreadSplittableRandomAdapter() throws SeedException {
    this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(Long.BYTES));
  }

  /**
   * Use the provided seed for the {@link SplittableRandom}.
   *
   * @param seed The seed. Must be 8 bytes.
   */
  public SingleThreadSplittableRandomAdapter(final byte[] seed) {
    super(seed);
  }

  /**
   * Use the provided seed for the {@link SplittableRandom}.
   *
   * @param seed The seed.
   */
  public SingleThreadSplittableRandomAdapter(final long seed) {
    super(seed);
  }

  /**
   * Must be redeclared in this package so that {@link ReseedingSplittableRandomAdapter} can access
   * it.
   */
  @Override protected void debitEntropy(final long bits) {
    super.debitEntropy(bits);
  }

  /**
   * Returns this SingleThreadSplittableRandomAdapter's only {@link SplittableRandom}.
   *
   * @return the SplittableRandom
   */
  @Override protected SplittableRandom getSplittableRandom() {
    return delegate;
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    setSeed(seed);
  }

}
