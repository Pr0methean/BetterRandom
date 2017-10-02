package io.github.pr0methean.betterrandom.prng.adapter;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Random;
import java.util.SplittableRandom;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

/**
 * {@link BaseSplittableRandomAdapter} where {@link #setSeed(long)} and {@link #setSeed(byte[])}
 * replace the {@link SplittableRandom} that would be used in the same calling context to generate
 * the random numbers.
 *
 * @author Chris Hennick
 */
public abstract class DirectSplittableRandomAdapter extends BaseSplittableRandomAdapter {

  private static final long serialVersionUID = 4273652147052638879L;
  @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
  protected transient SplittableRandom underlying; // a SplittableRandom is not Serializable

  /**
   * Constructs an instance with the given seed.
   *
   * @param seed The seed.
   */
  public DirectSplittableRandomAdapter(final byte[] seed) {
    super(seed);
    setSeedInternal(seed);
  }

  /**
   * Constructs an instance with the given seed.
   *
   * @param seed The seed.
   */
  @EntryPoint
  public DirectSplittableRandomAdapter(final long seed) {
    super(seed);
    setSeed(seed);
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
    if (seed.length != Long.BYTES) {
      throw new IllegalArgumentException("DirectSplittableRandomAdapter requires an 8-byte seed");
    }
    super.setSeedInternal(seed);
    underlying = new SplittableRandom(BinaryUtils.convertBytesToLong(seed));
  }

  @Override
  public void setSeed(@UnknownInitialization DirectSplittableRandomAdapter this,
      final long seed) {
    super.setSeedInternal(BinaryUtils.convertLongToBytes(seed));
    underlying = new SplittableRandom(seed);
  }
}
