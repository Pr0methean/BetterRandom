package io.github.pr0methean.betterrandom.prng.adapter;

import static io.github.pr0methean.betterrandom.prng.adapter.SplittableRandomReseeder.canGetSeed;
import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertBytesToLong;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
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

  private static final int SEED_LENGTH_BITS = SEED_LENGTH_BYTES * 8;
  private static final long serialVersionUID = 2190439512972880590L;
  @SuppressWarnings("ThreadLocalNotStaticFinal")
  private transient ThreadLocal<SplittableRandom> splittableRandoms;
  private transient ThreadLocal<AtomicLong> entropyBits;
  private transient ThreadLocal<byte[]> seeds;

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
  protected void recordEntropySpent(final long bits) {
    entropyBits.get().addAndGet(-bits);
  }

  @Override
  protected void recordAllEntropySpent() {
    entropyBits.get().set(0);
  }

  @EnsuresNonNull({"splittableRandoms", "entropyBits", "seeds"})
  @RequiresNonNull({"lock", "underlying"})
  private void initSubclassTransientFields(
      @UnknownInitialization(BaseSplittableRandomAdapter.class)SplittableRandomAdapter this) {
    lock.lock();
    try {
      splittableRandoms = ThreadLocal.withInitial(underlying::split);
      entropyBits = ThreadLocal.withInitial(() -> new AtomicLong(SEED_LENGTH_BITS));

      // If necessary, getSeed() will return the master seed on each thread where setSeed() yet
      // hasn't been called
      seeds = ThreadLocal.withInitial(canGetSeed()
          ? () -> SplittableRandomReseeder.getSeed(splittableRandoms.get())
          : () -> seed);
    } finally {
      lock.unlock();
    }
    // WTF Checker Framework? Why is this needed?
    assert splittableRandoms != null : "@AssumeAssertion(nullness)";
    assert entropyBits != null : "@AssumeAssertion(nullness)";
    assert seeds != null : "@AssumeAssertion(nullness)";
  }

  @Override
  protected SplittableRandom getSplittableRandom() {
    return splittableRandoms.get();
  }

  @Override
  protected ToStringHelper addSubSubclassFields(final ToStringHelper original) {
    return original.add("splittableRandoms", splittableRandoms);
  }

  @Override
  public byte[] getSeed() {
    return seeds.get();
  }

  /**
   * {@inheritDoc} Applies only to the calling thread.
   */
  @Override
  public void setSeed(@UnknownInitialization SplittableRandomAdapter this, final byte[] seed) {
    if (seed.length != SEED_LENGTH_BYTES) {
      throw new IllegalArgumentException("SplittableRandomAdapter requires an 8-byte seed");
    }
    setSeed(convertBytesToLong(seed));
  }

  /**
   * {@inheritDoc} Applies only to the calling thread.
   */
  @Override
  public void setSeed(@UnknownInitialization SplittableRandomAdapter this,
      final long seed) {
    if (this.seed == null) {
      super.setSeed(seed);
    }
    if (splittableRandoms != null) {
      splittableRandoms.set(SplittableRandomReseeder.reseed(splittableRandoms.get(), seed));
      if (entropyBits != null) {
        entropyBits.get().updateAndGet(oldValue -> Math.max(oldValue, SEED_LENGTH_BITS));
      }
      if (seeds != null) {
        seeds.set(BinaryUtils.convertLongToBytes(seed));
      }
    }
  }
}
