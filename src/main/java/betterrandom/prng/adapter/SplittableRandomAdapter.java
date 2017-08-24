package betterrandom.prng.adapter;

import betterrandom.seed.SeedException;
import betterrandom.seed.SeedGenerator;
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

  @SuppressWarnings("contracts.postcondition.not.satisfied") // WTF?!
  @EnsuresNonNull({"threadLocal", "underlying"})
  @RequiresNonNull({"seed", "lock"})
  @Override
  protected void initSubclassTransientFields(
      @UnknownInitialization SplittableRandomAdapter this) {
    lock.lock();
    try {
      super.initSubclassTransientFields();
      threadLocal = ThreadLocal.withInitial(underlying::split);
    } finally {
      lock.unlock();
    }
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
    if (superConstructorFinished && threadLocal != null) {
      threadLocal.set(new SplittableRandom(seed));
    }
  }

  @Override
  public boolean equals(Object o) {
    return this == o
        || (o instanceof SplittableRandomAdapter
        && super.equals(o));
  }

  @Override
  public int hashCode() {
    return super.hashCode() + 1;
  }
}
