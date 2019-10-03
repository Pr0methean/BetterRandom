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
 * take its entropy below a minimum amount, and will instead either wait to be reseeded by a {@link
 * RandomSeederThread} or, if none is installed, reseed itself on the calling thread with a {@link
 * SeedGenerator}. If neither is present, the caller is responsible for reseeding, and any call that
 * would reduce entropy below the minimum will throw {@link IllegalStateException}.
 */
public class EntropyBlockingRandomWrapper extends RandomWrapper {

  private final long minimumEntropy;
  private final Condition seedingStatusChanged = lock.newCondition();

  /** Used on the calling thread when there isn't a working RandomSeederThread. */
  private final AtomicReference<SeedGenerator> sameThreadSeedGen;
  private transient volatile boolean waitingOnReseed = false;

  public EntropyBlockingRandomWrapper(long minimumEntropy, SeedGenerator seedGenerator)
      throws SeedException {
    super(seedGenerator);
    sameThreadSeedGen = new AtomicReference<>(seedGenerator);
    this.minimumEntropy = minimumEntropy;
    checkMaxOutputAtOnce();
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

  private void checkMaxOutputAtOnce() {
    long maxOutputAtOnce = 8 * entropyBits.get() - minimumEntropy;
    if (maxOutputAtOnce < Long.SIZE) {
      throw new IllegalArgumentException("Need to be able to output 64 bits at once");
    }
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
      onSeedingStateChanged();
    } finally {
      lock.unlock();
    }
  }

  @Override protected void debitEntropy(long bits) {
    while (entropyBits.addAndGet(-bits) < minimumEntropy) {
      SeedGenerator seedGenerator;
      lock.lock();
      try {
        RandomSeederThread seeder = randomSeeder.get();
        if (seeder != null) {
          waitingOnReseed = true;
          seeder.reseedAsync(this);
          seedingStatusChanged.await();
          waitingOnReseed = false;
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

  @Override public void setSeed(long seed) {
    if (lock == null) {
      super.setSeed(seed);
      return;
    }
    lock.lock();
    try {
      super.setSeed(seed);
      seedingStatusChanged.signalAll();
    } finally {
      lock.unlock();
    }
  }

  @Override public boolean needsReseedingEarly() {
    return waitingOnReseed;
  }
}
