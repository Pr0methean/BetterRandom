package betterrandom.prng.adapter;

import betterrandom.seed.SeedException;
import betterrandom.seed.SeedGenerator;
import betterrandom.util.BinaryUtils;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.SplittableRandom;

public class SingleThreadSplittableRandomAdapter extends DirectSplittableRandomAdapter {

  private static final long serialVersionUID = -1125374167384636394L;

  public SingleThreadSplittableRandomAdapter(SeedGenerator seedGenerator) throws SeedException {
    this(seedGenerator.generateSeed(SEED_LENGTH_BYTES));
  }

  public SingleThreadSplittableRandomAdapter(byte[] seed) {
    super(seed);
    initSubclassTransientFields();
  }


  // Overridden in the subclass
  @Override
  protected SplittableRandom getSplittableRandom() {
    return underlying;
  }

  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    ois.defaultReadObject();
    initTransientFields();
    initSubclassTransientFields();
    if (!deserializedAndNotUsedSince) {
      underlying = underlying.split(); // Ensures we aren't rewinding
    }
    deserializedAndNotUsedSince = true; // Ensures serializing and deserializing is idempotent
  }

  @Override
  public synchronized void setSeed(long seed) {
    if (!superConstructorFinished) {
      return; // Cannot work when called from Random.<init>
    }
    underlying = new SplittableRandom(seed);
    super.setSeed(BinaryUtils.convertLongToBytes(seed));
  }

  @Override
  public synchronized void setSeed(byte[] seed) {
    underlying = new SplittableRandom(BinaryUtils.convertBytesToLong(seed, 0));
    super.setSeed(seed);
  }

  @Override
  public boolean equals(Object o) {
    return this == o
        || (o instanceof SingleThreadSplittableRandomAdapter
        && underlying.equals(((SingleThreadSplittableRandomAdapter) o).underlying));
  }

  @Override
  public int hashCode() {
    return underlying.hashCode() + 1;
  }
}
