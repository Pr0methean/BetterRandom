package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.SplittableRandom;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
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
    // WTF Checker Framework? Why is this needed?
    assert threadLocal != null : "@AssumeAssertion(nullness)";
  }

  @Override
  protected SplittableRandom getSplittableRandom() {
    return threadLocal.get();
  }

  @Override
  public ToStringHelper addSubclassFields(ToStringHelper original) {
    return original
        .add("threadLocal", threadLocal)
        .add("underlying", underlying);
  }

  /**
   * {@inheritDoc} Applies only to the calling thread.
   */
  @Override
  public void setSeed(@UnknownInitialization SplittableRandomAdapter this, long seed) {
    this.seed = BinaryUtils.convertLongToBytes(seed);
    if (superConstructorFinished && threadLocal != null) {
      threadLocal.set(new SplittableRandom(seed));
    }
  }
}
