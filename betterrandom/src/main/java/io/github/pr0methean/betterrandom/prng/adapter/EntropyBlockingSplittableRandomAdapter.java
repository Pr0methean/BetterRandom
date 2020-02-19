package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.prng.EntropyBlockingHelper;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;

/**
 * A {@link SplittableRandomAdapter} that reseeds itself when its entropy drops too low. If created
 * with no {@link io.github.pr0methean.betterrandom.seed.SimpleRandomSeeder}, reseeding is done on
 * the thread that consumes pseudorandomness. Entropy count is thread-local, so consuming entropy on
 * one thread won't directly cause blocking on another thread.
 *
 * @deprecated Use {@link EntropyBlockingReseedingSplittableRandomAdapter}.
 */
@Deprecated
public class EntropyBlockingSplittableRandomAdapter extends SplittableRandomAdapter {
  private static final long serialVersionUID = 4992825526245524633L;
  private transient ThreadLocal<EntropyBlockingHelper> helpers;
  private final AtomicReference<SeedGenerator> sameThreadSeedGen;
  private final long minimumEntropy;

  /**
   * Creates an instance.
   *
   * @param minimumEntropy the minimum entropy; when an operation would otherwise drop the entropy
   *     below this amount, the PRNG for the calling thread will be reseeded first. Should generally
   *     be zero or negative.
   * @param seedGenerator the seed generator; used both to initialize the master
   *     {@link java.util.SplittableRandom} and for reseeding
   */
  public EntropyBlockingSplittableRandomAdapter(long minimumEntropy, SeedGenerator seedGenerator) {
    super(seedGenerator);
    this.minimumEntropy = minimumEntropy;
    sameThreadSeedGen = new AtomicReference<>(seedGenerator);
    initSubclassTransientFields();
  }

  /**
   * Creates an instance.
   *
   * @param seed the seed for the master {@link java.util.SplittableRandom}, which will be split to
   *     generate an instance for each thread; must be 8 bytes
   * @param minimumEntropy the minimum entropy; when an operation would otherwise drop the entropy
   *     below this amount, the PRNG for the calling thread will be reseeded first. Should generally
   *     be zero or negative.
   * @param sameThreadSeedGen the seed generator
   */
  public EntropyBlockingSplittableRandomAdapter(byte[] seed, long minimumEntropy,
      @Nullable SeedGenerator sameThreadSeedGen) {
    super(seed);
    this.minimumEntropy = minimumEntropy;
    this.sameThreadSeedGen = new AtomicReference<>(sameThreadSeedGen);
    initSubclassTransientFields();
    helpers.get().checkMaxOutputAtOnce();
  }

  /**
   * Creates an instance.
   *
   * @param seed the seed for the master {@link java.util.SplittableRandom}, which will be split to
   *     generate an instance for each thread
   * @param minimumEntropy the minimum entropy; when an operation would otherwise drop the entropy
   *     below this amount, the PRNG for the calling thread will be reseeded first. Should generally
   *     be zero or negative.
   * @param sameThreadSeedGen the seed generator
   */
  public EntropyBlockingSplittableRandomAdapter(long seed, long minimumEntropy,
      @Nullable SeedGenerator sameThreadSeedGen) {
    super(seed);
    this.minimumEntropy = minimumEntropy;
    this.sameThreadSeedGen = new AtomicReference<>(sameThreadSeedGen);
    initSubclassTransientFields();
    helpers.get().checkMaxOutputAtOnce();
  }

  private void initSubclassTransientFields() {
    helpers = ThreadLocal.withInitial(() -> {
      ThreadLocalFields threadLocalFields = this.threadLocalFields.get();
      return new EntropyBlockingHelper(minimumEntropy, this.sameThreadSeedGen, this,
          threadLocalFields.entropyBits, new ReentrantLock());
    });
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    initSubclassTransientFields();
  }

  @Override protected void debitEntropy(long bits) {
    helpers.get().debitEntropy(bits);
  }

  @Override public void setSeed(long seed) {
    if (helpers == null) {
      super.setSeed(seed);
      return;
    }
    lock.lock();
    try {
      super.setSeed(seed);
      helpers.get().onSeedingStateChanged(true);
    } finally {
      lock.unlock();
    }
  }
}
