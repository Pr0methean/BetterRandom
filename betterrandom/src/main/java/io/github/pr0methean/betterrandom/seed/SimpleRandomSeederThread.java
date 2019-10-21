package io.github.pr0methean.betterrandom.seed;

import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import io.github.pr0methean.betterrandom.EntropyCountingRandom;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.LooperThread;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread that loops over {@link ByteArrayReseedableRandom} instances and reseeds them. No {@link
 * EntropyCountingRandom} will be reseeded when it's already had more input than output.
 *
 * @author Chris Hennick
 */
public class SimpleRandomSeederThread extends LooperThread {
  protected static final Map<ByteArrayReseedableRandom, byte[]> SEED_ARRAYS =
      Collections.synchronizedMap(new WeakHashMap<>(1));
  protected static final long POLL_INTERVAL = 60;
  private static final long serialVersionUID = -4339570810679373476L;
  protected final SeedGenerator seedGenerator;
  private final byte[] longSeedArray = new byte[8];
  protected transient Set<ByteArrayReseedableRandom> byteArrayPrngs;
  protected transient Set<ByteArrayReseedableRandom> byteArrayPrngsThisIteration;
  protected transient Condition waitWhileEmpty;
  protected transient Condition waitForEntropyDrain;

  public SimpleRandomSeederThread(ThreadFactory factory, final SeedGenerator seedGenerator) {
    super(factory);
    this.seedGenerator = seedGenerator;
    initTransientFields();
  }

  public SimpleRandomSeederThread(SeedGenerator seedGenerator) {
    this(new SimpleRandomSeederThread.DefaultThreadFactory(seedGenerator.toString()), seedGenerator);
  }

  static boolean stillDefinitelyHasEntropy(final Object random) {
    if (!(random instanceof EntropyCountingRandom)) {
      return false;
    }
    EntropyCountingRandom entropyCountingRandom = (EntropyCountingRandom) random;
    return !entropyCountingRandom.needsReseedingEarly() &&
        entropyCountingRandom.getEntropyBits() > 0;
  }

  /**
   * Removes PRNGs so that they will no longer be reseeded.
   * @param randoms the PRNGs to remove
   */
  public void remove(Random... randoms) {
    remove(Arrays.asList(randoms));
  }

  /**
   * Removes PRNGs so that they will no longer be reseeded.
   * @param randoms the PRNGs to remove
   */
  public void remove(Collection<? extends Random> randoms) {
    if (randoms.size() == 0) {
      return;
    }
    lock.lock();
    try {
      byteArrayPrngs.removeAll(randoms);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Adds {@link ByteArrayReseedableRandom} instances.
   * @param randoms the PRNGs to start reseeding
   */
  public void add(ByteArrayReseedableRandom... randoms) {
    add(Arrays.asList(randoms));
  }

  /**
   * Adds {@link ByteArrayReseedableRandom} instances.
   * @param randoms the PRNGs to start reseeding
   */
  public void add(Collection<? extends ByteArrayReseedableRandom> randoms) {
    if (randoms.size() == 0) {
      return;
    }
    lock.lock();
    try {
      byteArrayPrngs.addAll(randoms);
      wakeUp();
    } finally {
      lock.unlock();
    }
  }

  public void wakeUp() {
    start();
    if (lock.tryLock()) {
      try {
        waitWhileEmpty.signalAll();
        waitForEntropyDrain.signalAll();
      } finally {
        lock.unlock();
      }
    }
  }

  protected static Logger getLogger() {
    return LoggerFactory.getLogger(RandomSeederThread.class);
  }

  @Override public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SimpleRandomSeederThread that = (SimpleRandomSeederThread) o;
    return seedGenerator.equals(that.seedGenerator) && factory.equals(that.factory);
  }

  @Override public int hashCode() {
    return 31 * seedGenerator.hashCode() + factory.hashCode();
  }

  protected void initTransientFields() {
    byteArrayPrngs = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>(1)));
    byteArrayPrngsThisIteration = Collections.newSetFromMap(new WeakHashMap<>(1));
    waitWhileEmpty = lock.newCondition();
    waitForEntropyDrain = lock.newCondition();
  }

  @SuppressWarnings({"InfiniteLoopStatement", "ObjectAllocationInLoop", "AwaitNotInLoop"}) @Override
  protected boolean iterate() {
    try {
      while (true) {
        byteArrayPrngsThisIteration.addAll(byteArrayPrngs);
        if (!byteArrayPrngsThisIteration.isEmpty()) {
          break;
        }
      }
      boolean entropyConsumed = reseedByteArrayReseedableRandoms();
      if (!entropyConsumed) {
        waitForEntropyDrain.await(POLL_INTERVAL, TimeUnit.SECONDS);
      }
      return true;
    } catch (final Throwable t) {
      getLogger().error("Disabling the RandomSeederThread for " + seedGenerator, t);
      return false;
    }
  }

  protected boolean reseedByteArrayReseedableRandoms() {
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
          byte[] seedArray = getSeedArray(random);
          seedGenerator.generateSeed(seedArray);
          random.setSeed(seedArray);
        }
      }
    } finally {
      byteArrayPrngsThisIteration.clear();
    }
    return entropyConsumed;
  }

  protected byte[] getSeedArray(ByteArrayReseedableRandom random) {
    return SimpleRandomSeederThread.SEED_ARRAYS
                .computeIfAbsent(random, random_ -> new byte[random_.getNewSeedLength()]);
  }

  protected void reseedWithLong(final Random random) {
    seedGenerator.generateSeed(longSeedArray);
    random.setSeed(BinaryUtils.convertBytesToLong(longSeedArray));
  }

  /**
   * Returns true if no {@link Random} instances are registered with this RandomSeederThread.
   *
   * @return true if no {@link Random} instances are registered with this RandomSeederThread.
   */
  public boolean isEmpty() {
    lock.lock();
    try {
      return byteArrayPrngs.isEmpty();
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

  public void reseedAsync(ByteArrayReseedableRandom random) {
    byteArrayPrngsThisIteration.add(random);
    wakeUp();
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    initTransientFields();
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

    public DefaultThreadFactory() {
      this("TODO"); // TODO
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
}
