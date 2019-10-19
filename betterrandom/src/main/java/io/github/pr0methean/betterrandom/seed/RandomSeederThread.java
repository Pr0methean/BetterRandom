package io.github.pr0methean.betterrandom.seed;

import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import io.github.pr0methean.betterrandom.EntropyCountingRandom;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Thread that loops over {@link Random} instances and reseeds them. No {@link
 * EntropyCountingRandom} will be reseeded when it's already had more input than output.
 *
 * @author Chris Hennick
 */
public final class RandomSeederThread extends SimpleRandomSeederThread {
  private transient Set<Random> otherPrngs;
  private transient Set<Random> otherPrngsThisIteration;
  private final long stopIfEmptyForNanos;

  private void initTransientFields() {
    byteArrayPrngs = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>(1)));
    otherPrngs = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>(1)));
    byteArrayPrngsThisIteration = Collections.newSetFromMap(new WeakHashMap<>(1));
    otherPrngsThisIteration = Collections.newSetFromMap(new WeakHashMap<>(1));
    waitWhileEmpty = lock.newCondition();
    waitForEntropyDrain = lock.newCondition();
  }

  public void reseedAsync(Random random) {
    if (random instanceof ByteArrayReseedableRandom) {
      byteArrayPrngsThisIteration.add((ByteArrayReseedableRandom) random);
    } else {
      otherPrngsThisIteration.add(random);
    }
    wakeUp();
  }

  @Override public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RandomSeederThread that = (RandomSeederThread) o;
    return seedGenerator.equals(that.seedGenerator) && factory.equals(that.factory);
  }

  @Override public int hashCode() {
    return 31 * seedGenerator.hashCode() + factory.hashCode();
  }

  /**
   * Returns the seed generator this RandomSeederThread is using.
   * @return the seed generator
   */
  public SeedGenerator getSeedGenerator() {
    return seedGenerator;
  }

  public void add(Random... randoms) {
    if (randoms.length == 0) {
      return;
    }
    lock.lock();
    try {
      for (final Random random : randoms) {
        if (random instanceof ByteArrayReseedableRandom) {
          byteArrayPrngs.add((ByteArrayReseedableRandom) random);
        } else {
          otherPrngs.add(random);
        }
      }
      wakeUp();
    } finally {
      lock.unlock();
    }
  }

  public RandomSeederThread(final SeedGenerator seedGenerator, ThreadFactory threadFactory) {
    this(seedGenerator, threadFactory, 5_000_000_000L);
  }

  public RandomSeederThread(final SeedGenerator seedGenerator, ThreadFactory threadFactory,
      long stopIfEmptyForNanos) {
    super(threadFactory, seedGenerator);
    Objects.requireNonNull(seedGenerator, "randomSeeder must not be null");
    this.stopIfEmptyForNanos = stopIfEmptyForNanos;
    initTransientFields();
    start();
  }

  /**
   * Creates an instance using a {@link DefaultThreadFactory}.
   *
   * @param seedGenerator the seed generator
   */
  public RandomSeederThread(final SeedGenerator seedGenerator) {
    this(seedGenerator, new DefaultThreadFactory("RandomSeederThread for " + seedGenerator));
  }

  @Override protected boolean iterate() {
    try {
      while (true) {
        otherPrngsThisIteration.addAll(otherPrngs);
        byteArrayPrngsThisIteration.addAll(byteArrayPrngs);
        if (!otherPrngsThisIteration.isEmpty() || !byteArrayPrngsThisIteration.isEmpty()) {
          break;
        }
        if (!waitWhileEmpty.await(stopIfEmptyForNanos, TimeUnit.NANOSECONDS)) {
          return false;
        }
      }
      boolean entropyConsumed = false;
      try {
        for (ByteArrayReseedableRandom random : byteArrayPrngsThisIteration) {
          if (stillDefinitelyHasEntropy(random)) {
            continue;
          }
          entropyConsumed = true;
          if (random.preferSeedWithLong()) {
            reseedWithLong((Random) random);
          } else {
            final byte[] seedArray = SEED_ARRAYS
                .computeIfAbsent(random, random_ -> new byte[random_.getNewSeedLength()]);
            seedGenerator.generateSeed(seedArray);
            random.setSeed(seedArray);
          }
        }
      } finally {
        byteArrayPrngsThisIteration.clear();
      }
      try {
        for (Random random : otherPrngsThisIteration) {
          if (!stillDefinitelyHasEntropy(random)) {
            entropyConsumed = true;
            reseedWithLong(random);
          }
        }
      } finally {
        otherPrngsThisIteration.clear();
      }
      if (!entropyConsumed) {
        waitForEntropyDrain.await(POLL_INTERVAL, TimeUnit.SECONDS);
      }
      return true;
    } catch (final Throwable t) {
      getLogger().error("Disabling the RandomSeederThread for " + seedGenerator, t);
      return false;
    }
  }

  /**
   * Shut down this thread even if {@link Random} instances are registered with it.
   */
  public void shutDown() {
    interrupt();
    clear();
  }

  private void clear() {
    lock.lock();
    try {
      unregisterWithAll(byteArrayPrngs);
      byteArrayPrngs.clear();
      byteArrayPrngsThisIteration.clear();
      unregisterWithAll(otherPrngs);
      otherPrngs.clear();
      otherPrngsThisIteration.clear();
    } finally {
      lock.unlock();
    }
  }

  private void unregisterWithAll(Set<?> randoms) {
    for (final Object random : randoms) {
      if (random instanceof BaseRandom) {
        try {
          ((BaseRandom) random).setRandomSeeder(null);
        } catch (UnsupportedOperationException ignored) {}
      }
    }
  }

  /**
   * Returns true if no {@link Random} instances are registered with this RandomSeederThread.
   *
   * @return true if no {@link Random} instances are registered with this RandomSeederThread.
   */
  public boolean isEmpty() {
    lock.lock();
    try {
      return byteArrayPrngs.isEmpty() && otherPrngs.isEmpty();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Shut down this thread if no {@link Random} instances are registered with it.
   */
  public void stopIfEmpty() {
    lock.lock();
    try {
      if (isEmpty()) {
        getLogger().info("Stopping empty RandomSeederThread for {}", seedGenerator);
        interrupt();
      }
    } finally {
      lock.unlock();
    }
  }

  @Override public String toString() {
    return String.format("RandomSeederThread (%s, %s)", seedGenerator, factory);
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    initTransientFields();
  }

}
