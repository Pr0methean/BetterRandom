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
  }

  public EntropyBlockingRandomWrapper(byte[] seed, long minimumEntropy,
      @Nullable SeedGenerator sameThreadSeedGen) {
    super(seed);
    this.minimumEntropy = minimumEntropy;
    this.sameThreadSeedGen = new AtomicReference<>(sameThreadSeedGen);
  }

  public EntropyBlockingRandomWrapper(long seed, long minimumEntropy,
      @Nullable SeedGenerator sameThreadSeedGen) {
    super(seed);
    this.minimumEntropy = minimumEntropy;
    this.sameThreadSeedGen = new AtomicReference<>(sameThreadSeedGen);
  }

  public EntropyBlockingRandomWrapper(Random wrapped, long minimumEntropy,
      @Nullable SeedGenerator sameThreadSeedGen) {
    super(wrapped);
    this.minimumEntropy = minimumEntropy;
    this.sameThreadSeedGen = new AtomicReference<>(sameThreadSeedGen);
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

  @Override public long nextLong(long origin, long bound) {
    long range = bound - origin;
    if (seed.length < Long.BYTES && range >= (1L << (8 * seed.length))) {
      // TODO
    }
    return super.nextLong(origin, bound);
  }

  @Override public double nextGaussian() {
    if (seed.length < Long.BYTES) {
      // TODO
    }
    return super.nextGaussian();
  }

  @Override public double nextDouble(double origin, double bound) {
    if (seed.length < Double.BYTES) {
      // TODO
    }
    return super.nextDouble(origin, bound);
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
