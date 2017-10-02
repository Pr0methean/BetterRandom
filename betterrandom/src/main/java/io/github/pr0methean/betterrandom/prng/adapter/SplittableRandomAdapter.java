package io.github.pr0methean.betterrandom.prng.adapter;

import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertBytesToLong;
import static org.checkerframework.checker.nullness.NullnessUtil.castNonNull;

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
 * @author Chris Hennick
 */
@SuppressWarnings("ThreadLocalNotStaticFinal")
public class SplittableRandomAdapter extends DirectSplittableRandomAdapter {

  private static final int SEED_LENGTH_BITS = Long.BYTES * 8;
  private static final long serialVersionUID = 2190439512972880590L;
  private transient ThreadLocal<SplittableRandom> splittableRandoms;
  private transient ThreadLocal<AtomicLong> entropyBits;
  private transient ThreadLocal<byte[]> seeds;

  /**
   * Use the provided seed generation strategy to create the seed for the master {@link
   * SplittableRandom}, which will be split to generate an instance for each thread.
   *
   * @param seedGenerator The seed generation strategy that will provide the seed value for this
   *     RNG.
   * @throws SeedException if there is a problem generating a seed.
   */
  public SplittableRandomAdapter(final SeedGenerator seedGenerator) throws SeedException {
    this(seedGenerator.generateSeed(Long.BYTES));
  }

  /**
   * Use the provided seed for the master {@link SplittableRandom}, which will be split to generate
   * an instance for each thread.
   *
   * @param seed The seed. Must be 8 bytes.
   */
  public SplittableRandomAdapter(byte[] seed) {
    super(seed);
    initSubclassTransientFields();
  }

  /**
   * Use the provided seed for the master {@link SplittableRandom}, which will be split to generate
   * an instance for each thread.
   *
   * @param seed The seed.
   */
  public SplittableRandomAdapter(long seed) {
    super(seed);
    initSubclassTransientFields();
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    lock = castNonNull(lock);
    underlying = castNonNull(underlying);
    initSubclassTransientFields();
  }

  @Override
  public long getEntropyBits() {
    return entropyBits.get().get();
  }

  @Override
  protected void recordEntropySpent(final long bits) {
    entropyBits.get().addAndGet(-bits);
  }

  @EnsuresNonNull({"splittableRandoms", "entropyBits", "seeds"})
  @RequiresNonNull({"lock", "underlying"})
  private void initSubclassTransientFields(
      @UnknownInitialization(BaseSplittableRandomAdapter.class)SplittableRandomAdapter this) {
    lock.lock();
    try {
      splittableRandoms = ThreadLocal.withInitial(underlying::split);
      entropyBits = ThreadLocal.withInitial(() -> new AtomicLong(SEED_LENGTH_BITS));

      // getSeed() will return the master seed on each thread where setSeed() hasn't yet been called
      seeds = ThreadLocal.withInitial(() -> seed);
    } finally {
      lock.unlock();
    }
    // WTF Checker Framework? Why is this needed?
    splittableRandoms = castNonNull(splittableRandoms);
    entropyBits = castNonNull(entropyBits);
    seeds = castNonNull(seeds);
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
    if (seed.length != Long.BYTES) {
      throw new IllegalArgumentException("SplittableRandomAdapter requires an 8-byte seed");
    }
    setSeed(convertBytesToLong(seed));
  }

  /**
   * {@inheritDoc} Applies only to the calling thread.
   */
  @SuppressWarnings("contracts.postcondition.not.satisfied")
  @Override
  public void setSeed(@UnknownInitialization SplittableRandomAdapter this,
      final long seed) {
    if (this.seed == null) {
      super.setSeed(seed);
    }
    if (splittableRandoms != null) {
      splittableRandoms.set(new SplittableRandom(seed));
      if (entropyBits != null) {
        entropyBits.get().updateAndGet(oldValue -> Math.max(oldValue, SEED_LENGTH_BITS));
      }
      if (seeds != null) {
        seeds.set(BinaryUtils.convertLongToBytes(seed));
      }
    }
  }
}
