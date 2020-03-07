package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.seed.RandomSeeder;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import javax.annotation.Nullable;

/**
 * A {@link RandomWrapper} with the additional property that it won't return any output that would
 * take its entropy below a minimum amount, and will instead either wait to be reseeded by a {@link
 * RandomSeeder} or, if none is installed, reseed itself on the calling thread with a {@link
 * SeedGenerator}. If neither is present, the caller is responsible for reseeding, and any call that
 * would reduce entropy below the minimum will throw {@link IllegalStateException}.
 */
public class EntropyBlockingRandomWrapper extends RandomWrapper {

  private static final long serialVersionUID = -853699062122154479L;
  private final long minimumEntropy;
  private final AtomicReference<SeedGenerator> sameThreadSeedGen;
  private final Condition seedingStatusChanged;
  private volatile transient boolean waitingOnReseed;

  public EntropyBlockingRandomWrapper(long minimumEntropy, SeedGenerator seedGenerator)
      throws SeedException {
    super(seedGenerator);
    this.minimumEntropy = minimumEntropy;
    sameThreadSeedGen = new AtomicReference<>(seedGenerator);
    this.seedingStatusChanged = this.lock.newCondition();
    checkMaxOutputAtOnce();
  }

  public EntropyBlockingRandomWrapper(byte[] seed, long minimumEntropy,
      @Nullable SeedGenerator sameThreadSeedGen) {
    super(seed);
    this.minimumEntropy = minimumEntropy;
    this.sameThreadSeedGen = new AtomicReference<>(sameThreadSeedGen);
    this.seedingStatusChanged = this.lock.newCondition();
    checkMaxOutputAtOnce();
  }

  public EntropyBlockingRandomWrapper(long seed, long minimumEntropy,
      @Nullable SeedGenerator sameThreadSeedGen) {
    super(seed);
    this.minimumEntropy = minimumEntropy;
    this.sameThreadSeedGen = new AtomicReference<>(sameThreadSeedGen);
    this.seedingStatusChanged = this.lock.newCondition();
    checkMaxOutputAtOnce();
  }

  public EntropyBlockingRandomWrapper(Random wrapped, long minimumEntropy,
      @Nullable SeedGenerator sameThreadSeedGen) {
    super(wrapped);
    this.minimumEntropy = minimumEntropy;
    this.sameThreadSeedGen = new AtomicReference<>(sameThreadSeedGen);
    this.seedingStatusChanged = this.lock.newCondition();
    checkMaxOutputAtOnce();
  }

  @Override public void nextBytes(byte[] bytes) {
    for (int i = 0; i < bytes.length; i++) {
      debitEntropy(Byte.SIZE);
      bytes[i] = (byte) (nextInt(1 << Byte.SIZE));
    }
  }

  @Nullable public SeedGenerator getSameThreadSeedGen() {
    return sameThreadSeedGen.get();
  }

  public void setSameThreadSeedGen(@Nullable SeedGenerator newSeedGen) {
    if (sameThreadSeedGen.getAndSet(newSeedGen) != newSeedGen) {
      onSeedingStateChanged(false);
    }
  }

  @Override protected void setSeedInternal(byte[] seed) {
    super.setSeedInternal(seed);
    onSeedingStateChanged(true);
 }

  @Override public void setRandomSeeder(@Nullable RandomSeeder randomSeeder) {
    super.setRandomSeeder(randomSeeder);
    onSeedingStateChanged(false);
  }

  @Override protected void debitEntropy(long bits) {
    long remaining;
    while (true) {
      remaining = entropyBits.addAndGet(-bits);
      if (remaining >= minimumEntropy) {
        if (remaining <= 0) {
          // We need reseeding, but don't need to block waiting for it
          RandomSeeder seeder = getRandomSeeder();
          if (seeder != null) {
            seeder.wakeUp();
          }
        }
        return;
      }
      lock.lock();
      try {
        RandomSeeder seeder = getRandomSeeder();
        if (seeder != null) {
          waitingOnReseed = true;
          seeder.wakeUp();
          try {
            seedingStatusChanged.await();
          } catch (InterruptedException ignored) {
            // Retry
          }
          continue;
        }
        SeedGenerator seedGenerator = sameThreadSeedGen.get();
        if (seedGenerator == null) {
          throw new IllegalStateException("Out of entropy and no way to reseed");
        }
        // Reseed on calling thread
        int newSeedLength = getNewSeedLength();
        byte[] newSeed = seed.length == newSeedLength ? seed : new byte[newSeedLength];
        seedGenerator.generateSeed(newSeed);
        setSeed(newSeed);
        waitingOnReseed = false;
      } finally {
        lock.unlock();
      }
    }
  }

  @Override public void setSeed(long seed) {
    if (seedingStatusChanged == null) {
      super.setSeed(seed);
      return;
    }
    lock.lock();
    try {
      super.setSeed(seed);
      onSeedingStateChanged(true);
    } finally {
      lock.unlock();
    }
  }

  @Override public boolean needsReseedingEarly() {
    return waitingOnReseed;
  }

  /**
   * Ensures that the attached PRNG can output 64 bits between reseedings, given that its current
   * entropy is the maximum possible. Methods such as {@link Random#nextLong()} and
   * {@link Random#nextDouble()} would become much slower and more complex if we didn't require this.
   *
   * @throws IllegalArgumentException if the PRNG cannot output 64 bits between reseedings
   */
  protected void checkMaxOutputAtOnce() {
    long maxOutputAtOnce = 8 * entropyBits.get() - minimumEntropy;
    if (maxOutputAtOnce < Long.SIZE) {
      throw new IllegalArgumentException("Need to be able to output 64 bits at once");
    }
  }

  /**
   * Called when a new seed generator or {@link RandomSeeder} is attached or a
   * new seed is generated, so that operations can unblock.
   *
   * @param reseeded true if the seed has changed; false otherwise
   */
  protected void onSeedingStateChanged(boolean reseeded) {
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
}
