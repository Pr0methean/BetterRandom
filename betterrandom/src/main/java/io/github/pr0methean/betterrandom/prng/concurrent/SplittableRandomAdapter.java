package io.github.pr0methean.betterrandom.prng.concurrent;

import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertBytesToLong;

import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeederThread;
import io.github.pr0methean.betterrandom.util.Java8Constants;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.atomic.AtomicLong;
import java8.util.SplittableRandom;
import javax.annotation.Nullable;

/**
 * Thread-safe PRNG that wraps a {@link ThreadLocal}&lt;{@link SplittableRandom}&gt;. Reseeding this
 * will only affect the calling thread, so this can't be used with a {@link RandomSeederThread}.
 * Instead, use a {@link ReseedingSplittableRandomAdapter}.
 *
 * @author Chris Hennick
 */
@SuppressWarnings("ThreadLocalNotStaticFinal") public class SplittableRandomAdapter
    extends DirectSplittableRandomAdapter {

  private static final int SEED_LENGTH_BITS = Java8Constants.LONG_BYTES * 8;
  private static final long serialVersionUID = 2190439512972880590L;

  protected static class ThreadLocalFields {
    public SplittableRandom splittableRandom;
    final public AtomicLong entropyBits;

    public ThreadLocalFields(SplittableRandom splittableRandom, long entropyBits) {
      this.splittableRandom = splittableRandom;
      this.entropyBits = new AtomicLong(entropyBits);
    }
  }

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
    this(seedGenerator.generateSeed(Java8Constants.LONG_BYTES));
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
    this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(Java8Constants.LONG_BYTES));
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
      // Kludge for Java 7's lack of updateAndGet. Should be safe since entropyBits is thread-local.
      threadLocalFields.get().entropyBits.set(seedLength * Java8Constants.LONG_BYTES);
    }
  }

  private void initSubclassTransientFields() {
    lock.lock();
    try {
      threadLocalFields = new ThreadLocal<ThreadLocalFields>() {
        @Override public ThreadLocalFields initialValue() {
          // Necessary because SplittableRandom.split() isn't itself thread-safe.
          lock.lock();
          try {
            return new ThreadLocalFields(delegate.split(), SEED_LENGTH_BITS);
          } finally {
            lock.unlock();
          }
        }
      };
    } finally {
      lock.unlock();
    }
    // WTF Checker Framework? Why is this needed?
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
  @Override public void setRandomSeeder(@Nullable final SimpleRandomSeederThread randomSeeder) {
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
    checkLength(seed, Java8Constants.LONG_BYTES);
    setSeed(convertBytesToLong(seed));
  }

  /**
   * {@inheritDoc} Applies only to the calling thread.
   */
  @Override public void setSeed(final long seed) {
    if (this.seed == null) {
      super.setSeed(seed);
    }
    if (threadLocalFields == null) {
      return;
    }
    ThreadLocalFields threadLocalFields = this.threadLocalFields.get();
    threadLocalFields.splittableRandom = new SplittableRandom(seed);
    creditEntropyForNewSeed(Java8Constants.LONG_BYTES);
  }
}
