package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.util.BinaryUtils;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.SplittableRandom;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

public abstract class DirectSplittableRandomAdapter extends BaseSplittableRandomAdapter {

  private static final long serialVersionUID = 4273652147052638879L;
  @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
  protected transient SplittableRandom underlying; // a SplittableRandom is not Serializable

  public DirectSplittableRandomAdapter(byte[] seed) {
    super(seed);
    setSeedInternal(seed);
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    setSeedInternal(seed);
  }

  @EnsuresNonNull({"this.seed", "underlying"})
  @Override
  protected void setSeedInternal(
      @UnknownInitialization(Random.class)DirectSplittableRandomAdapter this,
      byte[] seed) {
    if (seed.length != 8) {
      throw new IllegalArgumentException("DirectSplittableRandomAdapter requires an 8-byte seed");
    }
    super.setSeedInternal(seed);
    underlying = new SplittableRandom(
        BinaryUtils.convertBytesToLong(seed, 0));
  }

  @Override
  public boolean equals(Object o) {
    return this == o
        || (o != null
        && getClass() == o.getClass()
        && Arrays.equals(getSeed(), ((SplittableRandomAdapter) o).getSeed()));
  }
}
