package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeederThread;
import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import javax.annotation.Nullable;

/**
 * Provides common functionality used by {@link io.github.pr0methean.betterrandom.prng.concurrent.EntropyBlockingRandomWrapper}
 * and thread-locally by {@link io.github.pr0methean.betterrandom.prng.concurrent.EntropyBlockingSplittableRandomAdapter}.
 * Internally coupled with {@link BaseRandom} because the use of this class is a substitute for
 * mixin inheritance.
 */
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

  /**
   * Creates an instance.
   *
   * @param minimumEntropy the minimum entropy that must be present at the end of an operation that
   *     consumes entropy. Should generally be zero or negative.
   * @param sameThreadSeedGen a reference that will hold a seed generator that can be used on the
   *     calling thread when there is no {@link SimpleRandomSeederThread}
   * @param random the PRNG
   * @param entropyBits the PRNG's entropy counter
   * @param lock the lock to hold while reseeding the PRNG
   */
  public EntropyBlockingHelper(long minimumEntropy, AtomicReference<SeedGenerator> sameThreadSeedGen,
      BaseRandom random, final AtomicLong entropyBits, Lock lock) {
    this.minimumEntropy = minimumEntropy;
    this.random = random;
    this.lock = lock;
    this.seedingStatusChanged = this.lock.newCondition();
    this.sameThreadSeedGen = sameThreadSeedGen;
    this.entropyBits = entropyBits;
  }

  /**
   * Sets a {@link SeedGenerator} to use on the calling thread when no {@link SimpleRandomSeederThread}
   * is attached.
   *
   * @param newSeedGen the new {@link SeedGenerator}
   */
  public void setSameThreadSeedGen(@Nullable SeedGenerator newSeedGen) {
    if (sameThreadSeedGen.getAndSet(newSeedGen) != newSeedGen) {
      onSeedingStateChanged(false);
    }
  }

  /**
   * Ensures that the attached PRNG can output 64 bits between reseedings, given that its current
   * entropy is the maximum possible. Methods such as {@link Random#nextLong()} and
   * {@link Random#nextDouble()} would become much slower and more complex if we didn't require this.
   *
   * @throws IllegalArgumentException if the PRNG cannot output 64 bits between reseedings
   */
  public void checkMaxOutputAtOnce() {
    long maxOutputAtOnce = 8 * entropyBits.get() - minimumEntropy;
    if (maxOutputAtOnce < Long.SIZE) {
      throw new IllegalArgumentException("Need to be able to output 64 bits at once");
    }
  }

  /**
   * Called when a new seed generator or {@link SimpleRandomSeederThread} is attached or a
   * new seed is generated, so that operations can unblock.
   *
   * @param reseeded true if the seed has changed; false otherwise
   */
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

  /**
   * Record that entropy has been spent, and reseed if that takes the entropy count below the
   * minimum.
   *
   * @param bits The number of bits of entropy spent.
   */
  public void debitEntropy(long bits) {
    while (entropyBits.addAndGet(-bits) < minimumEntropy) {
      SeedGenerator seedGenerator;
      lock.lock();
      try {
        SimpleRandomSeederThread seeder = random.getRandomSeeder();
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
        seedGenerator = sameThreadSeedGen.get();
      } finally {
        lock.unlock();
      }
      if (seedGenerator == null) {
        throw new IllegalStateException("Out of entropy and no way to reseed");
      }
      waitingOnReseed = false;
      // Reseed on calling thread
      int newSeedLength = random.getNewSeedLength();
      byte[] newSeed =
          random.seed.length == newSeedLength ? random.seed : new byte[newSeedLength];
      reseedSameThread(seedGenerator, newSeed);
    }
  }

  private void reseedSameThread(SeedGenerator seedGenerator, byte[] newSeed) {
    seedGenerator.generateSeed(newSeed);
    random.setSeed(newSeed);
  }

  /**
   * @return true if a caller is blocked on the reseeding of this PRNG; false otherwise
   */
  public boolean isWaitingOnReseed() {
    return waitingOnReseed;
  }

  /**
   * Updates the entropy count to reflect a reseeding. Sets it to the seed length or the internal
   * state size, whichever is shorter, but never less than the existing entropy count.
   *
   * @param seedLength the length of the new seed in bytes
   */
  public void creditEntropyForNewSeed(int seedLength) {
    final long effectiveBits = Math.min(seedLength, random.getNewSeedLength()) * 8L;
    entropyBits.updateAndGet(oldCount -> Math.max(oldCount, effectiveBits));
  }
}
