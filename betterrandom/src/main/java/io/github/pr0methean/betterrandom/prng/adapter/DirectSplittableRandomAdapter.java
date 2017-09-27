package io.github.pr0methean.betterrandom.prng.adapter;

import static org.checkerframework.checker.nullness.NullnessUtil.castNonNull;

import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Random;
import java.util.SplittableRandom;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

/**
 * <p>Abstract DirectSplittableRandomAdapter class.</p>
 *
 * @author ubuntu
 * @version $Id: $Id
 */
public abstract class DirectSplittableRandomAdapter extends BaseSplittableRandomAdapter {

  private static final long serialVersionUID = 4273652147052638879L;
  @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
  protected transient SplittableRandom underlying; // a SplittableRandom is not Serializable

  /**
   * <p>Constructor for DirectSplittableRandomAdapter.</p>
   *
   * @param seed an array of byte.
   */
  public DirectSplittableRandomAdapter(final byte[] seed) {
    super(seed);
    setSeedInternal(seed);
  }

  public DirectSplittableRandomAdapter(long seed) {
    super(seed);
    setSeedInternal(seed);
  }

  @Override
  protected ToStringHelper addSubSubclassFields(final ToStringHelper original) {
    return original.add("underlying", underlying);
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    setSeedInternal(seed);
  }

  @EnsuresNonNull({"this.seed", "underlying", "entropyBits"})
  @Override
  protected void setSeedInternal(
      @UnknownInitialization(Random.class)DirectSplittableRandomAdapter this,
      final byte[] seed) {
    if (seed.length != 8) {
      throw new IllegalArgumentException("DirectSplittableRandomAdapter requires an 8-byte seed");
    }
    super.setSeedInternal(seed);
    System.arraycopy(seed, 0, longSeedArray, 0, Long.BYTES);
    underlying = new SplittableRandom(longSeedBuffer.getLong(0));
  }

  @EnsuresNonNull({"this.seed", "underlying", "entropyBits"})
  protected void setSeedInternal(
      @UnknownInitialization(Random.class)DirectSplittableRandomAdapter this, final long seed) {
    longSeedBuffer.putLong(0, seed);
    super.setSeedInternal(longSeedArray);
    underlying = new SplittableRandom(seed);
  }

  @Override
  public boolean preferSeedWithLong() {
    return true;
  }

  @Override
  public void setSeed(@UnknownInitialization DirectSplittableRandomAdapter this,
      final long seed) {
    if (superConstructorFinished) {
      castNonNull(longSeedBuffer).putLong(seed);
      super.setSeedInternal(castNonNull(longSeedArray));
      underlying = new SplittableRandom(seed);
    } else {
      fallbackSetSeed();
    }
  }
}
