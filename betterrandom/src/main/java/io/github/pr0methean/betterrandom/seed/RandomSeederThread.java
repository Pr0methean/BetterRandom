package io.github.pr0methean.betterrandom.seed;

import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import io.github.pr0methean.betterrandom.EntropyCountingRandom;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread that loops over {@link Random} instances and reseeds them. No {@link
 * EntropyCountingRandom} will be reseeded when it's already had more input than output.
 * @author Chris Hennick
 */
@SuppressWarnings("ClassExplicitlyExtendsThread")
public final class RandomSeederThread extends RandomSeederThreadTransients implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(RandomSeederThread.class);
  private static final long POLL_INTERVAL = 60;

  public void wakeUp() {
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
        byteArrayPrngs.remove(random);
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
      if (isDead()) {
        return;
      }
      for (final Random random : randoms) {
        if (random instanceof ByteArrayReseedableRandom) {
          byteArrayPrngs.add((ByteArrayReseedableRandom) random);
        } else {
          otherPrngs.add(random);
        }
      }
      waitForEntropyDrain.signalAll();
      waitWhileEmpty.signalAll();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RandomSeederThread that = (RandomSeederThread) o;
    return seedGenerator.equals(that.seedGenerator)
        && factory.equals(that.factory);
  }

  @Override
  public int hashCode() {
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

    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable, name);
      thread.setDaemon(true);
      thread.setPriority(priority);
      return thread;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      DefaultThreadFactory that = (DefaultThreadFactory) o;
      return priority == that.priority &&
          name.equals(that.name);
    }

    @Override
    public int hashCode() {
      return 31 * priority + name.hashCode();
    }
  }

  private final SeedGenerator seedGenerator;

  private final byte[] longSeedArray = new byte[8];

  private static Map<ByteArrayReseedableRandom, byte[]> SEED_ARRAYS =
      Collections.synchronizedMap(new WeakHashMap<>(1));

  public RandomSeederThread(final SeedGenerator seedGenerator, ThreadFactory threadFactory) {
    super(threadFactory);
    Objects.requireNonNull(seedGenerator, "randomSeeder must not be null");
    this.seedGenerator = seedGenerator;
    start();
  }

  /**
   * Creates an instance using a {@link DefaultThreadFactory}.
   * @param seedGenerator the seed generator
   */
  public RandomSeederThread(final SeedGenerator seedGenerator) {
    this(seedGenerator, new DefaultThreadFactory("RandomSeederThread for " + seedGenerator));
  }

  private boolean isDead() {
    return (getState() == Thread.State.TERMINATED) || isInterrupted();
  }

  @SuppressWarnings({"InfiniteLoopStatement", "ObjectAllocationInLoop", "AwaitNotInLoop"}) @Override
  protected boolean iterate() {
    try {
      while (true) {
        otherPrngsThisIteration.addAll(otherPrngs);
        byteArrayPrngsThisIteration.addAll(byteArrayPrngs);
        if (otherPrngsThisIteration.isEmpty() && byteArrayPrngsThisIteration.isEmpty()) {
          waitWhileEmpty.await();
        } else {
          break;
        }
      }
      boolean entropyConsumed = false;
      final Iterator<ByteArrayReseedableRandom> byteArrayPrngsIterator =
          byteArrayPrngsThisIteration.iterator();
      while (byteArrayPrngsIterator.hasNext()) {
        final ByteArrayReseedableRandom random = byteArrayPrngsIterator.next();
        byteArrayPrngsIterator.remove();
        if (stillDefinitelyHasEntropy(random)) {
          continue;
        }
        entropyConsumed = true;
        if (random.preferSeedWithLong()) {
          reseedWithLong((Random) random);
        } else {
          final byte[] seedArray =
              SEED_ARRAYS.computeIfAbsent(random, random_ -> new byte[random_.getNewSeedLength()]);
          seedGenerator.generateSeed(seedArray);
          random.setSeed(seedArray);
        }
      }
      final Iterator<Random> otherPrngsIterator = otherPrngsThisIteration.iterator();
      while (otherPrngsIterator.hasNext()) {
        final Random random = otherPrngsIterator.next();
        otherPrngsIterator.remove();
        if (stillDefinitelyHasEntropy(random)) {
          continue;
        }
        entropyConsumed = true;
        reseedWithLong(random);
      }
      if (!entropyConsumed) {
        waitForEntropyDrain.await(POLL_INTERVAL, TimeUnit.SECONDS);
      }
      return true;
    } catch (final Throwable t) {
      LOG.error("Disabling the RandomSeederThread for " + seedGenerator, t);
      shutDown();
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

  private static boolean stillDefinitelyHasEntropy(final Object random) {
    return (random instanceof EntropyCountingRandom) &&
        (((EntropyCountingRandom) random).getEntropyBits() > 0);
  }

  private void clear() {
    lock.lock();
    try {
      for (final ByteArrayReseedableRandom random : byteArrayPrngs) {
        if (random instanceof BaseRandom) {
          ((BaseRandom) random).setRandomSeeder((RandomSeederThread) null);
        }
      }
      byteArrayPrngs.clear();
      byteArrayPrngsThisIteration.clear();
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
        LOG.info("Stopping empty RandomSeederThread for {}", seedGenerator);
        shutDown();
      }
    } finally {
      lock.unlock();
    }
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    lock.lock();
    try {
      out.defaultWriteObject();
    } finally {
      lock.unlock();
    }
  }
}
