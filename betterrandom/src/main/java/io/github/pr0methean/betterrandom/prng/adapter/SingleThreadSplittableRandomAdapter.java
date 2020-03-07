package io.github.pr0methean.betterrandom.prng.adapter;

import com.google.common.base.MoreObjects;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
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
public class SingleThreadSplittableRandomAdapter extends BaseSplittableRandomAdapter {

  private static final long serialVersionUID = -1125374167384636394L;

  /**
   * The master {@link SplittableRandom} that will either be delegated to directly (see {@link
   * SingleThreadSplittableRandomAdapter} or be split using {@link SplittableRandom#split()} (see
   * {@link SplittableRandomAdapter}) and have the splits delegated to.
   */
  @SuppressWarnings(
      "InstanceVariableMayNotBeInitializedByReadObject") protected transient volatile SplittableRandom
      delegate; // SplittableRandom isn't Serializable

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
    setSeed(seed);
  }

  /**
   * Must be redeclared in this package so that {@link SplittableRandomAdapter} can access
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

  /**
   * Replaces the delegate with a new {@link SplittableRandom} that uses the given seed.
   *
   * @param seed the new seed
   */
  @Override public void setSeed(final long seed) {
    if (superConstructorFinished) {
      super.setSeedInternal(BinaryUtils.convertLongToBytes(seed));
    }
    delegate = new SplittableRandom(seed);
  }

  /**
   * Replaces the delegate with a new {@link SplittableRandom} that uses the given seed.
   *
   * @param seed the new seed; must be 8 bytes
   */
  @Override protected void setSeedInternal(final byte[] seed) {
    super.setSeedInternal(seed);
    delegate = new SplittableRandom(BinaryUtils.convertBytesToLong(seed));
  }

  @Override protected MoreObjects.ToStringHelper addSubclassFields(final MoreObjects.ToStringHelper original) {
    return original.add("delegate", delegate);
  }
}
