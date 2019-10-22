package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeederThread;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import javax.annotation.Nullable;

public class EntropyBlockingHelper implements Serializable {
  private static final long serialVersionUID = 8061321755747974708L;
  private final long minimumEntropy;
  private final Lock lock;
  private final Condition seedingStatusChanged;
  /** Used on the calling thread when there isn't a working RandomSeederThread. */
  private final AtomicReference<SeedGenerator> sameThreadSeedGen;
  private final AtomicLong entropyBits;
  private final BaseRandom random;



  @SuppressWarnings("TransientFieldNotInitialized") private transient volatile boolean waitingOnReseed = false;

  public EntropyBlockingHelper(long minimumEntropy, AtomicReference<SeedGenerator> sameThreadSeedGen,
      BaseRandom random, final AtomicLong entropyBits, Lock lock) {
    this.minimumEntropy = minimumEntropy;
    this.random = random;
    this.lock = lock;
    this.seedingStatusChanged = this.lock.newCondition();
    this.sameThreadSeedGen = sameThreadSeedGen;
    this.entropyBits = entropyBits;
  }

  public SeedGenerator getSameThreadSeedGenerator() {
    return sameThreadSeedGen.get();
  }

  public void setSameThreadSeedGen(@Nullable SeedGenerator newSeedGen) {
    if (sameThreadSeedGen.getAndSet(newSeedGen) != newSeedGen) {
      onSeedingStateChanged(false);
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
        SimpleRandomSeederThread seeder = random.getRandomSeeder();
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
      }
      // Reseed on calling thread
      int newSeedLength = random.getNewSeedLength();
      byte[] newSeed =
          random.seed.length == newSeedLength ? random.seed : new byte[newSeedLength];
      reseedSameThread(seedGenerator, newSeed);
    }
  }

  public void reseedSameThread(SeedGenerator seedGenerator, byte[] newSeed) {
    seedGenerator.generateSeed(newSeed);
    random.setSeed(newSeed);
  }

  public void awaitReseedingBy(SimpleRandomSeederThread seeder) {
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

  public void creditEntropyForNewSeed(int seedLength) {
    final long effectiveBits = Math.min(seedLength, random.getNewSeedLength()) * 8L;
    entropyBits.updateAndGet(oldCount -> Math.max(oldCount, effectiveBits));
  }
}
