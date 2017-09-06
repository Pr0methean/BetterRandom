package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Random;
import java.util.SplittableRandom;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

public class SingleThreadSplittableRandomAdapter extends DirectSplittableRandomAdapter {

  private static final long serialVersionUID = -1125374167384636394L;
  private boolean deserializedAndNotUsedSince = false;

  public SingleThreadSplittableRandomAdapter(SeedGenerator seedGenerator) throws SeedException {
    this(seedGenerator.generateSeed(SEED_LENGTH_BYTES));
  }

  public SingleThreadSplittableRandomAdapter(byte[] seed) {
    super(seed);
  }

  @Override
  public ToStringHelper addSubclassFields(ToStringHelper original) {
    return original
        .add("underlying", underlying);
  }

  @Override
  protected SplittableRandom getSplittableRandom() {
    deserializedAndNotUsedSince = false;
    return underlying;
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    setSeed(seed);
    if (!deserializedAndNotUsedSince) {
      underlying = underlying.split(); // Ensures we aren't rewinding
      deserializedAndNotUsedSince = true; // Ensures serializing and deserializing is idempotent
    }
  }

  @EnsuresNonNull({"this.seed", "underlying"})
  @Override
  public synchronized void setSeed(@UnknownInitialization SingleThreadSplittableRandomAdapter this,
      long seed) {
    underlying = SplittableRandomReseeder.reseed(underlying, seed);
    this.seed = BinaryUtils.convertLongToBytes(seed);
  }

  @EnsuresNonNull({"this.seed", "underlying"})
  @Override
  public void setSeedInitial(
      @UnknownInitialization(Random.class)SingleThreadSplittableRandomAdapter this,
      byte[] seed) {
    if (seed.length != 8) {
      throw new IllegalArgumentException("DirectSplittableRandomAdapter requires an 8-byte seed");
    }
    setSeed(BinaryUtils.convertBytesToLong(seed, 0));
  }
}
