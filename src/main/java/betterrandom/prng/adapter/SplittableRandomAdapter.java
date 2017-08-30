package betterrandom.prng.adapter;

import betterrandom.seed.SeedException;
import betterrandom.seed.SeedGenerator;
import betterrandom.util.BinaryUtils;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.SplittableRandom;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/**
 * Thread-safe version of {@link SingleThreadSplittableRandomAdapter}.
 */
public class SplittableRandomAdapter extends DirectSplittableRandomAdapter {

  private static final long serialVersionUID = 2190439512972880590L;
  @SuppressWarnings("ThreadLocalNotStaticFinal")
  private transient ThreadLocal<SplittableRandom> threadLocal;

  public SplittableRandomAdapter(SeedGenerator seedGenerator) throws SeedException {
    super(seedGenerator.generateSeed(SEED_LENGTH_BYTES));
    initSubclassTransientFields();
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    assert seed != null : "@AssumeAssertion(nullness)";
    assert lock != null : "@AssumeAssertion(nullness)";
    assert underlying != null : "@AssumeAssertion(nullness)";
    initSubclassTransientFields();
  }

  @EnsuresNonNull({"threadLocal"})
  @RequiresNonNull({"lock", "underlying"})
  private void initSubclassTransientFields(
      @UnknownInitialization(BaseSplittableRandomAdapter.class)SplittableRandomAdapter this) {
    lock.lock();
    try {
      threadLocal = ThreadLocal.withInitial(underlying::split);
    } finally {
      lock.unlock();
    }
    assert threadLocal != null : "@AssumeAssertion(nullness)";
  }

  @Override
  protected SplittableRandom getSplittableRandom() {
    return threadLocal.get();
  }

  /**
   * {@inheritDoc} Applies only to the calling thread.
   */
  @SuppressWarnings("NonSynchronizedMethodOverridesSynchronizedMethod")
  @Override
  public void setSeed(@UnknownInitialization SplittableRandomAdapter this, long seed) {
    this.seed = BinaryUtils.convertLongToBytes(seed);
    if (superConstructorFinished && threadLocal != null) {
      threadLocal.set(new SplittableRandom(seed));
    }
  }
}
