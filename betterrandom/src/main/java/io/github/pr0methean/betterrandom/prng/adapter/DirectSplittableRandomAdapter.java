package io.github.pr0methean.betterrandom.prng.adapter;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.SplittableRandom;

/**
 * Abstract subclass of {@link BaseSplittableRandomAdapter} where {@link #setSeed(long)} and {@link
 * #setSeed(byte[])} replace the {@link SplittableRandom} that's used in the context in which they
 * are called. See {@link ReseedingSplittableRandomAdapter} for an example of when it does
 * <i>not</i> make sense to extend this class.
 * @author Chris Hennick
 */
public abstract class DirectSplittableRandomAdapter extends BaseSplittableRandomAdapter {

  private static final long serialVersionUID = 4273652147052638879L;

  /**
   * The master {@link SplittableRandom} that will either be delegated to directly (see {@link
   * SingleThreadSplittableRandomAdapter} or be split using {@link SplittableRandom#split()} (see
   * {@link SplittableRandomAdapter}) and have the splits delegated to.
   */
  @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
  protected transient SplittableRandom underlying; // a SplittableRandom is not Serializable

  /**
   * Wraps a {@link SplittableRandom} with the specified seed.
   * @param seed 8 bytes of seed data used to initialise the RNG.
   */
  protected DirectSplittableRandomAdapter(final byte[] seed) {
    super(seed);
  }

  /**
   * Wraps a {@link SplittableRandom} with the specified seed.
   * @param seed the seed.
   */
  @EntryPoint protected DirectSplittableRandomAdapter(final long seed) {
    super(seed);
    setSeed(seed);
  }

  @Override protected ToStringHelper addSubclassFields(final ToStringHelper original) {
    return original.add("underlying", underlying);
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    setSeedInternal(seed);
  }

  /**
   * Replaces {@link #underlying} with a new {@link SplittableRandom} that uses the given seed.
   */
  @Override protected void setSeedInternal(final byte[] seed) {
    if (seed.length != Long.BYTES) {
      throw new IllegalArgumentException("DirectSplittableRandomAdapter requires an 8-byte seed");
    }
    super.setSeedInternal(seed);
    underlying = new SplittableRandom(BinaryUtils.convertBytesToLong(seed));
  }

  /**
   * Replaces {@link #underlying} with a new {@link SplittableRandom} that uses the given seed.
   */
  @Override public void setSeed(final long seed) {
    if (superConstructorFinished) {
      super.setSeedInternal(BinaryUtils.convertLongToBytes(seed));
    }
    underlying = new SplittableRandom(seed);
  }
}
