package io.github.pr0methean.betterrandom.prng.adapter;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.SplittableRandom;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/**
 * Thread-safe version of {@link io.github.pr0methean.betterrandom.prng.adapter.SingleThreadSplittableRandomAdapter}.
 *
 * @author ubuntu
 * @version $Id: $Id
 */
public class SplittableRandomAdapter extends DirectSplittableRandomAdapter {

  private static final long serialVersionUID = 2190439512972880590L;
  @SuppressWarnings("ThreadLocalNotStaticFinal")
  private transient ThreadLocal<SplittableRandom> threadLocal;

  /**
   * <p>Constructor for SplittableRandomAdapter.</p>
   *
   * @param seedGenerator a {@link io.github.pr0methean.betterrandom.seed.SeedGenerator}
   *     object.
   * @throws io.github.pr0methean.betterrandom.seed.SeedException if any.
   */
  public SplittableRandomAdapter(final SeedGenerator seedGenerator) throws SeedException {
    super(seedGenerator.generateSeed(SEED_LENGTH_BYTES));
    initSubclassTransientFields();
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    assert lock != null : "@AssumeAssertion(nullness)";
    assert underlying != null : "@AssumeAssertion(nullness)";
    initSubclassTransientFields();
  }

  @EnsuresNonNull("threadLocal")
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

  /** {@inheritDoc} */
  @Override
  protected SplittableRandom getSplittableRandom() {
    return threadLocal.get();
  }

  /** {@inheritDoc} */
  @Override
  protected ToStringHelper addSubSubclassFields(final ToStringHelper original) {
    return original.add("threadLocal", threadLocal);
  }

  /**
   * {@inheritDoc} Applies only to the calling seederThread.
   */
  @Override
  public void setSeed(@UnknownInitialization SplittableRandomAdapter this,
      final long seed) {
    super.setSeed(seed);
    if (threadLocal != null) {
      threadLocal.set(SplittableRandomReseeder.reseed(threadLocal.get(), seed));
    }
  }
}
