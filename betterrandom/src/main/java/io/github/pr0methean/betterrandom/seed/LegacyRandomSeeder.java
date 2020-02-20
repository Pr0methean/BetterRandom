package io.github.pr0methean.betterrandom.seed;

import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * A {@link SimpleRandomSeeder} that can reseed any instance of {@link Random}.
 *
 * @author Chris Hennick
 */
public final class LegacyRandomSeeder extends SimpleRandomSeeder {
  private transient Set<Random> otherPrngs;
  private transient Set<Random> otherPrngsThisIteration;

  @Override protected void initTransientFields() {
    super.initTransientFields();
    otherPrngs = createSynchronizedWeakHashSet();
    otherPrngsThisIteration = createSynchronizedWeakHashSet();
  }

  @Override public void remove(Collection<? extends Random> randoms) {
    lock.lock();
    try {
      for (Random random : randoms) {
        if (random instanceof ByteArrayReseedableRandom) {
          byteArrayPrngs.remove(random);
        } else {
          otherPrngs.remove(random);
        }
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Adds {@link Random} instances.
   * @param randoms the PRNGs to start reseeding
   */
  public void add(Random... randoms) {
    addLegacyRandoms(Arrays.asList(randoms));
  }

  /**
   * Adds {@link Random} instances.
   * @param randoms the PRNGs to start reseeding
   */
  public void addLegacyRandoms(Collection<? extends Random> randoms) {
    if (randoms.size() == 0) {
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

  /**
   * Creates an instance whose thread will terminate if no PRNGs have been associated with it for 5
   * seconds.
   *
   * @param seedGenerator the seed generator
   * @param threadFactory the {@link ThreadFactory} that will create this seeder's thread
   */
  public LegacyRandomSeeder(final SeedGenerator seedGenerator, ThreadFactory threadFactory) {
    this(seedGenerator, threadFactory, DEFAULT_STOP_IF_EMPTY_FOR_NANOS);
  }

  /**
   * Creates an instance.
   *
   * @param seedGenerator the seed generator
   * @param threadFactory the {@link ThreadFactory} that will create this seeder's thread
   * @param stopIfEmptyForNanos time in nanoseconds after which this thread will terminate if no
   *     PRNGs are attached
   */
  public LegacyRandomSeeder(final SeedGenerator seedGenerator, ThreadFactory threadFactory,
      long stopIfEmptyForNanos) {
    super(seedGenerator, threadFactory, stopIfEmptyForNanos);
    initTransientFields();
    start();
  }

  /**
   * Creates an instance using a {@link DefaultThreadFactory}.
   *
   * @param seedGenerator the seed generator
   */
  public LegacyRandomSeeder(final SeedGenerator seedGenerator) {
    this(seedGenerator, new DefaultThreadFactory("LegacyRandomSeeder for " + seedGenerator));
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
      boolean entropyConsumed = reseedByteArrayReseedableRandoms();
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
      return false;
    }
  }

  @Override public boolean isEmpty() {
    lock.lock();
    try {
      return super.isEmpty() && otherPrngs.isEmpty();
    } finally {
      lock.unlock();
    }
  }

  @Override protected void clear() {
    lock.lock();
    try {
      super.clear();
      unregisterWithAll(otherPrngs);
      otherPrngs.clear();
      otherPrngsThisIteration.clear();
    } finally {
      lock.unlock();
    }
  }

  @Override public String toString() {
    return String.format("LegacyRandomSeeder (%s, %s)", seedGenerator, factory);
  }

}
