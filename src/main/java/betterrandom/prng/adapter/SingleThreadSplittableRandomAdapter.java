package betterrandom.prng.adapter;

import betterrandom.seed.SeedException;
import betterrandom.seed.SeedGenerator;
import betterrandom.util.BinaryUtils;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.SplittableRandom;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class SingleThreadSplittableRandomAdapter extends DirectSplittableRandomAdapter {

  private static final long serialVersionUID = -1125374167384636394L;
  protected boolean deserializedAndNotUsedSince = false;
  public SingleThreadSplittableRandomAdapter(SeedGenerator seedGenerator) throws SeedException {
    this(seedGenerator.generateSeed(SEED_LENGTH_BYTES));
  }

  public SingleThreadSplittableRandomAdapter(byte[] seed) {
    super(seed);
    initSubclassTransientFields();
  }

  @Override
  protected SplittableRandom getSplittableRandom() {
    deserializedAndNotUsedSince = false;
    return underlying;
  }

  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    ois.defaultReadObject();
    initTransientFields();
    setSeed(seed);
    initSubclassTransientFields();
    if (!deserializedAndNotUsedSince) {
      underlying = underlying.split(); // Ensures we aren't rewinding
    }
    deserializedAndNotUsedSince = true; // Ensures serializing and deserializing is idempotent
  }

  @Override
  public synchronized void setSeed(@UnknownInitialization SingleThreadSplittableRandomAdapter this,
      long seed) {
    underlying = new SplittableRandom(seed);
    this.seed = BinaryUtils.convertLongToBytes(seed);
  }

  @Override
  public void setSeed(@UnknownInitialization SingleThreadSplittableRandomAdapter this,
      byte[] seed) {
    underlying = new SplittableRandom(BinaryUtils.convertBytesToLong(seed, 0));
    this.seed = seed.clone();
  }
}
