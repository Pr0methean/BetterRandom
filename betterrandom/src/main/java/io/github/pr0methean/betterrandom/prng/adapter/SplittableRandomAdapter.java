package io.github.pr0methean.betterrandom.prng.adapter;

import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertBytesToLong;

import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeeder;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;

/**
 * Thread-safe PRNG that wraps a {@link ThreadLocal}&lt;{@link SplittableRandom}&gt;. Reseeding this
 * will only affect the calling thread, so this can't be used with a {@link SimpleRandomSeeder}.
 * Instead, use a {@link ReseedingSplittableRandomAdapter}.
 *
 * @deprecated In OpenJDK 8 and Android API 24 and later,
 * {@link java.util.concurrent.ThreadLocalRandom} uses
 * the same PRNG algorithm as {@link SplittableRandom}, and is faster because of internal coupling
 * with {@link Thread}. As well, the instance returned by
 * {@link java.util.concurrent.ThreadLocalRandom#current()} can be safely passed to any thread that
 * has ever called {@code current()}, and streams created by a ThreadLocalRandom are safely
 * parallel. For situations where reseeding or a specified seed is needed, use {@link
 * ReseedingSplittableRandomAdapter} (whose {@link SimpleRandomSeeder} parameter is now nullable).
 *
 * @author Chris Hennick
 */
@SuppressWarnings("ThreadLocalNotStaticFinal")
@Deprecated
public class SplittableRandomAdapter
    extends DirectSplittableRandomAdapter {

  private static final long serialVersionUID = 2190439512972880590L;

  /**
   * Holds the {@link SplittableRandom} and the entropy counter for a thread. Extracted into an
   * object so that only one {@link ThreadLocal} has to be accessed (a slow operation) when both of
   * these are needed.
   */
  protected static class ThreadLocalFields {
    /**
     * The {@link SplittableRandom} that generates random numbers when called on this thread.
     */
    public SplittableRandom splittableRandom;
    /**
     * Stores the entropy count for {@code splittableRandom}.
     */
    final public AtomicLong entropyBits;

    /**
     * Creates an instance for a given {@link SplittableRandom} that has not been used before.
     *
     * @param splittableRandom the {@link SplittableRandom}
     */
    public ThreadLocalFields(SplittableRandom splittableRandom) {
      this.splittableRandom = splittableRandom;
      this.entropyBits = new AtomicLong(Long.SIZE);
    }
  }

  /**
   * Holds the {@link SplittableRandom} and the entropy counter for each thread.
   */
  protected transient ThreadLocal<ThreadLocalFields> threadLocalFields;

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
  public SplittableRandomAdapter(final byte[] seed) {
    super(seed);
    initSubclassTransientFields();
  }

  /**
   * Use the {@link DefaultSeedGenerator} to generate a seed for the master {@link
   * SplittableRandom}, which will be split to generate an instance for each thread.
   *
   * @throws SeedException if the {@link DefaultSeedGenerator} fails to generate a seed.
   */
  public SplittableRandomAdapter() throws SeedException {
    this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(Long.BYTES));
  }

  /**
   * Use the provided seed for the master {@link SplittableRandom}, which will be split to generate
   * an instance for each thread.
   *
   * @param seed The seed.
   */
  public SplittableRandomAdapter(final long seed) {
    super(seed);
    initSubclassTransientFields();
  }

  @Override public boolean usesParallelStreams() {
    return true;
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    initSubclassTransientFields();
  }

  /**
   * Returns the entropy count for the calling thread (it is separate for each thread).
   */
  @Override public long getEntropyBits() {
    return threadLocalFields.get().entropyBits.get();
  }

  @Override protected void debitEntropy(final long bits) {
    threadLocalFields.get().entropyBits.addAndGet(-bits);
  }

  @Override protected void creditEntropyForNewSeed(final int seedLength) {
    if (threadLocalFields != null) {
      threadLocalFields.get().entropyBits.updateAndGet(oldCount -> Math.max(oldCount, Long.SIZE));
    }
  }

  private void initSubclassTransientFields() {
    lock.lock();
    try {
      threadLocalFields = ThreadLocal.withInitial(() -> {
        // Necessary because SplittableRandom.split() isn't itself thread-safe.
        lock.lock();
        try {
          return new ThreadLocalFields(delegate.split());
        } finally {
          lock.unlock();
        }
      });
    } finally {
      lock.unlock();
    }
  }

  @Override protected SplittableRandom getSplittableRandom() {
    return threadLocalFields.get().splittableRandom;
  }

  /**
   * Not supported, because this class uses a thread-local seed.
   *
   * @param randomSeeder ignored.
   * @throws UnsupportedOperationException always.
   */
  @Override public void setRandomSeeder(@Nullable final SimpleRandomSeeder randomSeeder) {
    if (randomSeeder != null) {
      throw new UnsupportedOperationException("Use ReseedingSplittableRandomAdapter instead");
    }
  }

  /**
   * {@inheritDoc} Returns the root seed, not the calling thread's seed.
   */
  @Override public byte[] getSeed() {
    return super.getSeed();
  }

  /**
   * {@inheritDoc} Applies only to the calling thread.
   */
  @Override public void setSeed(final byte[] seed) {
    checkLength(seed, Long.BYTES);
    setSeed(convertBytesToLong(seed));
  }

  /**
   * {@inheritDoc} Applies only to the calling thread.
   */
  @Override public void setSeed(
      final long seed) {
    if (this.seed == null) {
      super.setSeed(seed);
    }
    if (threadLocalFields == null) {
      return;
    }
    ThreadLocalFields threadLocalFields = this.threadLocalFields.get();
    threadLocalFields.splittableRandom = new SplittableRandom(seed);
    creditEntropyForNewSeed(8);
  }
}
