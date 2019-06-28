package io.github.pr0methean.betterrandom.seed;

import io.github.pr0methean.betterrandom.util.BinaryUtils;
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
@SuppressWarnings("ClassExplicitlyExtendsThread")
public final class RandomSeederThread extends LooperThread {
  private transient Set<Random> otherPrngs;
  private transient Set<Random> otherPrngsThisIteration;
  private transient Condition waitWhileEmpty;
  private static final Logger LOG = LoggerFactory.getLogger(RandomSeederThread.class);
  private static final long POLL_INTERVAL = 60;
  private static final long STOP_IF_EMPTY_FOR_SECONDS = 5;

  private void initTransientFields() {
    otherPrngs = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>(1)));
    otherPrngsThisIteration = Collections.newSetFromMap(new WeakHashMap<>(1));
    waitWhileEmpty = lock.newCondition();
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
    initTransientFields();
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
          reseedWithLong(random);
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

  private void reseedWithLong(final Random random) {
    seedGenerator.generateSeed(longSeedArray);
    random.setSeed(BinaryUtils.convertBytesToLong(longSeedArray));
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
