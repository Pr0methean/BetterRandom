package io.github.pr0methean.betterrandom.seed;

import io.github.pr0methean.betterrandom.util.LooperThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * Thread that loops over {@link Random} instances and reseeds them. (Simplified version for reproducing a bug.)
 * @author Chris Hennick
 */
public final class RandomSeederThread extends LooperThread {
  private static final long BITWISE_BYTE_TO_LONG = 0x000000FF;
  private transient Set<Random> otherPrngs;
  private transient Set<Random> otherPrngsThisIteration;
  private transient Condition waitWhileEmpty;
  private static final Logger LOG = LoggerFactory.getLogger(RandomSeederThread.class);
  private static final long STOP_IF_EMPTY_FOR_SECONDS = 5;

  /**
   * Convert a byte array to a long.
   * @param bytes a byte array of length {@link Long#BYTES} in
   *     {@link java.nio.ByteOrder#nativeOrder()} order.
   * @return {@code bytes} as a long.
   */
  private static long convertBytesToLong(final byte[] bytes) {
    return (BITWISE_BYTE_TO_LONG & bytes[7])
        | ((BITWISE_BYTE_TO_LONG & bytes[6]) << 8L)
        | ((BITWISE_BYTE_TO_LONG & bytes[5]) << 16L)
        | ((BITWISE_BYTE_TO_LONG & bytes[4]) << 24L)
        | ((BITWISE_BYTE_TO_LONG & bytes[3]) << 32L)
        | ((BITWISE_BYTE_TO_LONG & bytes[2]) << 40L)
        | ((BITWISE_BYTE_TO_LONG & bytes[1]) << 48L)
        | ((BITWISE_BYTE_TO_LONG & bytes[0]) << 56L);
  }

  public void add(Random... randoms) {
    if (randoms.length == 0) {
      return;
    }
    lock.lock();
    try {
      for (final Random random : randoms) {
        otherPrngs.add(random);
      }
      start();
      waitWhileEmpty.signalAll();
    } finally {
      lock.unlock();
    }
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

    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable, name);
      thread.setDaemon(true);
      thread.setPriority(priority);
      return thread;
    }
  }

  private final SeedGenerator seedGenerator;

  private final byte[] longSeedArray = new byte[8];

  public RandomSeederThread(final SeedGenerator seedGenerator, ThreadFactory threadFactory) {
    super(threadFactory);
    Objects.requireNonNull(seedGenerator, "randomSeeder must not be null");
    this.seedGenerator = seedGenerator;
    otherPrngs = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>(1)));
    otherPrngsThisIteration = Collections.newSetFromMap(new WeakHashMap<>(1));
    waitWhileEmpty = lock.newCondition();
    start();
  }

  /**
   * Creates an instance using a {@link DefaultThreadFactory}.
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
        if (otherPrngsThisIteration.isEmpty()) {
          if (!waitWhileEmpty.await(STOP_IF_EMPTY_FOR_SECONDS, TimeUnit.SECONDS)) {
            return false;
          }
        } else {
          break;
        }
      }
      try {
        for (Random random : otherPrngsThisIteration) {
          seedGenerator.generateSeed(longSeedArray);
          random.setSeed(convertBytesToLong(longSeedArray));
        }
      } finally {
        otherPrngsThisIteration.clear();
      }
      return true;
    } catch (final Throwable t) {
      LOG.error("Disabling the RandomSeederThread for " + seedGenerator, t);
      return false;
    }
  }

  private void shutDown() {
    interrupt();
    clear();
  }

  private void clear() {
    lock.lock();
    try {
      otherPrngs.clear();
      otherPrngsThisIteration.clear();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Returns true if no {@link Random} instances are registered with this RandomSeederThread.
   * @return true if no {@link Random} instances are registered with this RandomSeederThread.
   */
  public boolean isEmpty() {
    lock.lock();
    try {
      return otherPrngs.isEmpty();
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
        LOG.info("Stopping empty RandomSeederThread for {}", seedGenerator);
        shutDown();
      }
    } finally {
      lock.unlock();
    }
  }

}
