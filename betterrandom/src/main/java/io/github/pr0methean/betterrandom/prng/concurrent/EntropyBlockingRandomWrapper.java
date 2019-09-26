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

  /** Used on the calling thread when there isn't a working RandomSeederThread. */
  private final AtomicReference<SeedGenerator> sameThreadSeedGen;

  public EntropyBlockingRandomWrapper(long minimumEntropy, SeedGenerator seedGenerator)
      throws SeedException {
    super(seedGenerator);
    sameThreadSeedGen = new AtomicReference<>(seedGenerator);
    this.minimumEntropy = minimumEntropy;
    checkMaxOutputAtOnce();
  }

  private void checkMaxOutputAtOnce() {
    long maxOutputAtOnce = 8 * entropyBits.get() - minimumEntropy;
    if (maxOutputAtOnce < Long.SIZE) {
      throw new IllegalArgumentException("Need to be able to output 64 bits at once");
    }
  }

  public EntropyBlockingRandomWrapper(byte[] seed, long minimumEntropy,
      @Nullable SeedGenerator sameThreadSeedGen) {
    super(seed);
    this.minimumEntropy = minimumEntropy;
    this.sameThreadSeedGen = new AtomicReference<>(sameThreadSeedGen);
    checkMaxOutputAtOnce();
  }

  public EntropyBlockingRandomWrapper(long seed, long minimumEntropy,
      @Nullable SeedGenerator sameThreadSeedGen) {
    super(seed);
    this.minimumEntropy = minimumEntropy;
    this.sameThreadSeedGen = new AtomicReference<>(sameThreadSeedGen);
    checkMaxOutputAtOnce();
  }

  public EntropyBlockingRandomWrapper(Random wrapped, long minimumEntropy,
      @Nullable SeedGenerator sameThreadSeedGen) {
    super(wrapped);
    this.minimumEntropy = minimumEntropy;
    this.sameThreadSeedGen = new AtomicReference<>(sameThreadSeedGen);
    checkMaxOutputAtOnce();
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
    lock.lock();
    try {
      super.setRandomSeeder(randomSeeder);
    } finally {
      lock.unlock();
    }
    onSeedingStateChanged();
  }

  @Override protected void debitEntropy(long bits) {
    while (entropyBits.addAndGet(-bits) < minimumEntropy) {
      SeedGenerator seedGenerator;
      lock.lock();
      try {
        RandomSeederThread seeder = randomSeeder.get();
        if (seeder != null) {
          seeder.wakeUp();
          seedingStatusChanged.await();
          continue;
        }
        seedGenerator = sameThreadSeedGen.get();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      } finally {
        lock.unlock();
      }
      if (seedGenerator == null) {
        throw new IllegalStateException("Out of entropy and no way to reseed");
      } else {
        // Reseed on calling thread
        int newSeedLength = getNewSeedLength();
        byte[] newSeed;
        if (seed.length == newSeedLength) {
          newSeed = seed;
        } else {
          newSeed = new byte[newSeedLength];
        }
        seedGenerator.generateSeed(newSeed);
        setSeed(newSeed);
      }
    }
  }
}
