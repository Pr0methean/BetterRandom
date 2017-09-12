package io.github.pr0methean.betterrandom.prng.adapter;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicLong;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/**
 * Thread-safe version of {@link SingleThreadSplittableRandomAdapter}. Reseeding this will only
 * affect the calling thread, so this can't be used with a {@link io.github.pr0methean.betterrandom.seed.RandomSeederThread}.
 * Instead, use a {@link ReseedingSplittableRandomAdapter}.
 *
 * @author ubuntu
 * @version $Id: $Id
 */
public class SplittableRandomAdapter extends DirectSplittableRandomAdapter {

  public static final int SEED_LENGTH_BITS = SEED_LENGTH_BYTES * 8;
  private static final long serialVersionUID = 2190439512972880590L;
  @SuppressWarnings("ThreadLocalNotStaticFinal")
  private transient ThreadLocal<SplittableRandom> threadLocal;
  private transient ThreadLocal<AtomicLong> entropyBits;

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

  @Override
  public long entropyBits() {
    return entropyBits.get().get();
  }

  @Override
  protected void recordEntropySpent(long bits) {
    entropyBits.get().addAndGet(-bits);
  }

  @Override
  protected void recordAllEntropySpent() {
    entropyBits.get().set(0);
  }

  @EnsuresNonNull({"threadLocal", "entropyBits"})
  @RequiresNonNull({"lock", "underlying"})
  private void initSubclassTransientFields(
      @UnknownInitialization(BaseSplittableRandomAdapter.class)SplittableRandomAdapter this) {
    lock.lock();
    try {
      threadLocal = ThreadLocal.withInitial(underlying::split);
      entropyBits = ThreadLocal.withInitial(() -> new AtomicLong(SEED_LENGTH_BITS));
    } finally {
      lock.unlock();
    }
    // WTF Checker Framework? Why is this needed?
    assert threadLocal != null : "@AssumeAssertion(nullness)";
    assert entropyBits != null : "@AssumeAssertion(nullness)";
  }

  @Override
  protected SplittableRandom getSplittableRandom() {
    return threadLocal.get();
  }

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
      if (entropyBits != null) {
        entropyBits.get().updateAndGet(oldValue -> Math.max(oldValue, SEED_LENGTH_BITS));
      }
    }
  }
}
