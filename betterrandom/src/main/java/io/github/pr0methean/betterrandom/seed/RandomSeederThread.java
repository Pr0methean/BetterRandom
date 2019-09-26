package io.github.pr0methean.betterrandom.seed;

import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import io.github.pr0methean.betterrandom.EntropyCountingRandom;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.LooperThread;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread that loops over {@link Random} instances and reseeds them. No {@link
 * EntropyCountingRandom} will be reseeded when it's already had more input than output.
 *
 * @author Chris Hennick
 */
public final class RandomSeederThread extends LooperThread {
  private transient Set<ByteArrayReseedableRandom> byteArrayPrngs;
  private transient Set<Random> otherPrngs;
  private transient Set<ByteArrayReseedableRandom> byteArrayPrngsThisIteration;
  private transient Set<Random> otherPrngsThisIteration;
  private transient Condition waitWhileEmpty;
  private transient Condition waitForEntropyDrain;
  private static volatile Logger LOG;
  private static final long POLL_INTERVAL = 60;
  private final long stopIfEmptyForNanos;

  private static Logger getLogger() {
    if (LOG == null) {
      synchronized (RandomSeederThread.class) {
        if (LOG == null) {
          LOG = LoggerFactory.getLogger(RandomSeederThread.class);
        }
      }
    }
    return LOG;
  }

  private void initTransientFields() {
    byteArrayPrngs = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>(1)));
    otherPrngs = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>(1)));
    byteArrayPrngsThisIteration = Collections.newSetFromMap(new WeakHashMap<>(1));
    otherPrngsThisIteration = Collections.newSetFromMap(new WeakHashMap<>(1));
    waitWhileEmpty = lock.newCondition();
    waitForEntropyDrain = lock.newCondition();
  }

  public void wakeUp() {
    start();
    if (lock.tryLock()) {
      try {
        waitForEntropyDrain.signalAll();
      } finally {
        lock.unlock();
      }
    }
  }

  public void remove(Random... randoms) {
    if (randoms.length == 0) {
      return;
    }
    lock.lock();
    try {
      for (Random random : randoms) {
        if (random instanceof ByteArrayReseedableRandom) {
          byteArrayPrngs.remove(random);
        }
        otherPrngs.remove(random);
      }
    } finally {
      lock.unlock();
    }
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
      start();
      waitForEntropyDrain.signalAll();
      waitWhileEmpty.signalAll();
    } finally {
      lock.unlock();
    }
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

  public static class DefaultThreadFactory implements ThreadFactory, Serializable {

    private static final long serialVersionUID = -5806852086706570346L;
    private final String name;
    private final int priority;

    public DefaultThreadFactory(String name) {
      this(name, Thread.NORM_PRIORITY + 1);
    }

    public DefaultThreadFactory(String name, int priority) {
      this.name = name;
      this.priority = priority;
    }

    @Override public Thread newThread(Runnable runnable) {
      Thread thread = DEFAULT_THREAD_FACTORY.newThread(runnable);
      thread.setName(name);
      thread.setDaemon(true);
      thread.setPriority(priority);
      return thread;
    }

    @Override public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      DefaultThreadFactory that = (DefaultThreadFactory) o;
      return priority == that.priority && name.equals(that.name);
    }

    @Override public int hashCode() {
      return 31 * priority + name.hashCode();
    }
  }

  private final SeedGenerator seedGenerator;

  private final byte[] longSeedArray = new byte[8];

  private static final Map<ByteArrayReseedableRandom, byte[]> SEED_ARRAYS =
      Collections.synchronizedMap(new WeakHashMap<>(1));

  public RandomSeederThread(final SeedGenerator seedGenerator, ThreadFactory threadFactory) {
    this(seedGenerator, threadFactory, 5_000_000_000L);
  }

  public RandomSeederThread(final SeedGenerator seedGenerator, ThreadFactory threadFactory,
      long stopIfEmptyForNanos) {
    super(threadFactory);
    Objects.requireNonNull(seedGenerator, "randomSeeder must not be null");
    this.stopIfEmptyForNanos = stopIfEmptyForNanos;
    this.seedGenerator = seedGenerator;
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

  @SuppressWarnings({"InfiniteLoopStatement", "ObjectAllocationInLoop", "AwaitNotInLoop"}) @Override
  protected boolean iterate() {
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

  private void reseedWithLong(final Random random) {
    seedGenerator.generateSeed(longSeedArray);
    random.setSeed(BinaryUtils.convertBytesToLong(longSeedArray));
  }

  private static boolean stillDefinitelyHasEntropy(final Object random) {
    return (random instanceof EntropyCountingRandom) &&
        (((EntropyCountingRandom) random).getEntropyBits() > 0);
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
