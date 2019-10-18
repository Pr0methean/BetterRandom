package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import javax.annotation.Nullable;

public class EntropyBlockingHelper implements Serializable {
  private final long minimumEntropy;
  private final Lock lock;
  private final Condition seedingStatusChanged;
  /** Used on the calling thread when there isn't a working RandomSeederThread. */
  private final AtomicReference<SeedGenerator> sameThreadSeedGen;
  private final AtomicLong entropyBits;
  private final BaseRandom random;



  @SuppressWarnings("TransientFieldNotInitialized") private transient volatile boolean waitingOnReseed = false;

  public EntropyBlockingHelper(long minimumEntropy, AtomicReference<SeedGenerator> sameThreadSeedGen,
      BaseRandom random) {
    this.minimumEntropy = minimumEntropy;
    this.random = random;
    lock = random.lock;
    this.seedingStatusChanged = lock.newCondition();
    this.sameThreadSeedGen = sameThreadSeedGen;
    this.entropyBits = random.entropyBits;
  }

  public SeedGenerator getSameThreadSeedGenerator() {
    return sameThreadSeedGen.get();
  }

  public void setSameThreadSeedGen(@Nullable SeedGenerator newSeedGen) {
    if (sameThreadSeedGen.getAndSet(newSeedGen) != newSeedGen) {
      onSeedingStateChanged(waitingOnReseed);
    }
  }

  public void checkMaxOutputAtOnce() {
    long maxOutputAtOnce = 8 * entropyBits.get() - minimumEntropy;
    if (maxOutputAtOnce < Long.SIZE) {
      throw new IllegalArgumentException("Need to be able to output 64 bits at once");
    }
  }

  public void onSeedingStateChanged(boolean reseeded) {
    if (reseeded) {
      waitingOnReseed = false;
    }
    lock.lock();
    try {
      if (seedingStatusChanged != null) {
        seedingStatusChanged.signalAll();
      }
    } finally {
      lock.unlock();
    }
  }

  public void debitEntropy(long bits) {
    while (entropyBits.addAndGet(-bits) < minimumEntropy) {
      SeedGenerator seedGenerator;
      lock.lock();
      try {
        RandomSeederThread seeder = random.getRandomSeeder();
        if (seeder != null) {
          awaitReseedingBy(seeder);
          continue;
        }
        seedGenerator = sameThreadSeedGen.get();
      } finally {
        lock.unlock();
      }
      if (seedGenerator == null) {
        throw new IllegalStateException("Out of entropy and no way to reseed");
      } else {
        // Reseed on calling thread
        int newSeedLength = random.getNewSeedLength();
        byte[] newSeed;
        if (random.seed.length == newSeedLength) {
          newSeed = random.seed;
        } else {
          newSeed = new byte[newSeedLength];
        }
        seedGenerator.generateSeed(newSeed);
        random.setSeed(newSeed);
      }
    }
  }

  public void awaitReseedingBy(RandomSeederThread seeder) {
    waitingOnReseed = true;
    seeder.reseedAsync(random);
    try {
      seedingStatusChanged.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  public boolean isWaitingOnReseed() {
    return waitingOnReseed;
  }
}
