package io.github.pr0methean.betterrandom.prng.concurrent;

import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import javax.annotation.Nullable;

/**
 * A {@link RandomWrapper} with the additional property that it won't return any output that would
 * take its entropy below a minimum amount, and will instead
 */
public class EntropyBlockingRandomWrapper extends RandomWrapper {
  private final long minimumEntropy;
  private final Condition seedingStatusChanged = lock.newCondition();
  private final long maxOutputAtOnce;
  private static final double DOUBLE_ULP = 1.0 / ENTROPY_OF_DOUBLE;

  /** Used on the calling thread when there isn't a working RandomSeederThread. */
  private final AtomicReference<SeedGenerator> sameThreadSeedGen;

  public EntropyBlockingRandomWrapper(long minimumEntropy, SeedGenerator seedGenerator)
      throws SeedException {
    super(seedGenerator);
    sameThreadSeedGen = new AtomicReference<>(seedGenerator);
    this.minimumEntropy = minimumEntropy;
    maxOutputAtOnce = getNewSeedLength() - minimumEntropy;
  }

  public EntropyBlockingRandomWrapper(byte[] seed, long minimumEntropy,
      @Nullable SeedGenerator sameThreadSeedGen) {
    super(seed);
    this.minimumEntropy = minimumEntropy;
    this.sameThreadSeedGen = new AtomicReference<>(sameThreadSeedGen);
    maxOutputAtOnce = getNewSeedLength() - minimumEntropy;
  }

  public EntropyBlockingRandomWrapper(long seed, long minimumEntropy,
      @Nullable SeedGenerator sameThreadSeedGen) {
    super(seed);
    this.minimumEntropy = minimumEntropy;
    this.sameThreadSeedGen = new AtomicReference<>(sameThreadSeedGen);
    maxOutputAtOnce = getNewSeedLength() - minimumEntropy;
  }

  public EntropyBlockingRandomWrapper(Random wrapped, long minimumEntropy,
      @Nullable SeedGenerator sameThreadSeedGen) {
    super(wrapped);
    this.minimumEntropy = minimumEntropy;
    this.sameThreadSeedGen = new AtomicReference<>(sameThreadSeedGen);
    maxOutputAtOnce = getNewSeedLength() - minimumEntropy;
  }

  @Nullable public SeedGenerator getSameThreadSeedGen() {
    return sameThreadSeedGen.get();
  }

  public void setSameThreadSeedGen(@Nullable SeedGenerator newSeedGen) {
    if (sameThreadSeedGen.getAndSet(newSeedGen) != newSeedGen) {
      onSeedingStateChanged();
    }
  }

  private void onSeedingStateChanged() {
    lock.lock();
    try {
      if (seedingStatusChanged != null) {
        seedingStatusChanged.signalAll();
      }
    } finally {
      lock.unlock();
    }
  }

  @Override protected void setSeedInternal(byte[] seed) {
    super.setSeedInternal(seed);
    onSeedingStateChanged();
  }

  @Override public void setRandomSeeder(@Nullable RandomSeederThread randomSeeder) {
    super.setRandomSeeder(randomSeeder);
    onSeedingStateChanged();
  }

  @Override protected long nextLongNoEntropyDebit() {
    return ((long) nextInt()) << 32L | nextInt();
  }

  @Override public double nextDoubleNoEntropyDebit() {
    // Based on Apache Harmony's java.util.Random
    // https://github.com/apache/harmony/blob/02970cb7227a335edd2c8457ebdde0195a735733/classlib/modules/luni/src/main/java/java/util/Random.java#L140
    return (((long)(nextInt(1 << 27)) << 26) + nextInt(1 << 26)) * DOUBLE_ULP;
  }

  @Override protected void debitEntropy(long bits) {
    while (entropyBits.addAndGet(-bits) < minimumEntropy) {
      if (randomSeeder.get() == null) {
        SeedGenerator seedGenerator = sameThreadSeedGen.get();
        if (seedGenerator == null) {
          throw new IllegalStateException("Out of entropy and no way to reseed");
        } else {
          // Reseed on calling thread
          lock.lock();
          try {
            int newSeedLength = getNewSeedLength();
            byte[] newSeed;
            if (seed.length == newSeedLength) {
              newSeed = seed;
            } else {
              newSeed = new byte[newSeedLength];
            }
            seedGenerator.generateSeed(newSeed);
            setSeed(newSeed);
          } finally {
            lock.unlock();
          }
        }
      } else {
        lock.lock();
        try {
          seedingStatusChanged.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        } finally {
          lock.unlock();
        }
      }
    }
  }
}
