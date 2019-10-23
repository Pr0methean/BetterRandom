package io.github.pr0methean.betterrandom.seed;

import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import io.github.pr0methean.betterrandom.EntropyCountingRandom;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.LooperThread;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
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
 * Thread that loops over {@link ByteArrayReseedableRandom} instances and reseeds them. No {@link
 * EntropyCountingRandom} will be reseeded when it's already had more input than output.
 *
 * @author Chris Hennick
 */
public class SimpleRandomSeederThread extends LooperThread {
  protected static final Map<ByteArrayReseedableRandom, byte[]> SEED_ARRAYS =
      Collections.synchronizedMap(new WeakHashMap<>(1));
  // FIXME: Setting a longer POLL_INTERVAL slows many tests, and causes some to time out
  // (Why doesn't BaseRandom's call to reseedAsync() prevent this?!)
  protected static final long POLL_INTERVAL = 1;
  private static final long serialVersionUID = -4339570810679373476L;
  protected final SeedGenerator seedGenerator;
  private final byte[] longSeedArray = new byte[8];
  protected transient Set<ByteArrayReseedableRandom> byteArrayPrngs;
  protected transient Set<ByteArrayReseedableRandom> byteArrayPrngsThisIteration;
  protected transient Condition waitWhileEmpty;
  protected transient Condition waitForEntropyDrain;
  protected final long stopIfEmptyForNanos;
  public SimpleRandomSeederThread(final SeedGenerator seedGenerator, ThreadFactory factory) {
    this(seedGenerator, factory, 5_000_000_000L);
  }

  public SimpleRandomSeederThread(SeedGenerator seedGenerator) {
    this(seedGenerator, new SimpleRandomSeederThread.DefaultThreadFactory(seedGenerator.toString()));
  }

  public SimpleRandomSeederThread(SeedGenerator seedGenerator, ThreadFactory factory,
      long stopIfEmptyForNanos) {
    super(factory);
    this.seedGenerator = seedGenerator;
    Objects.requireNonNull(seedGenerator, "randomSeeder must not be null");

    this.stopIfEmptyForNanos = stopIfEmptyForNanos;
    initTransientFields();
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
        waitForEntropyDrain.signalAll();
        waitWhileEmpty.signalAll();
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
    byteArrayPrngs = createSynchronizedHashSet();
    byteArrayPrngsThisIteration = createSynchronizedHashSet();
    waitWhileEmpty = lock.newCondition();
    waitForEntropyDrain = lock.newCondition();
  }

  protected <T> Set<T> createSynchronizedHashSet() {
    return Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<T, Boolean>(1)));
  }

  @SuppressWarnings({"InfiniteLoopStatement", "ObjectAllocationInLoop", "AwaitNotInLoop"}) @Override
  protected boolean iterate() {
    try {
      while (true) {
        byteArrayPrngsThisIteration.addAll(byteArrayPrngs);
        if (!byteArrayPrngsThisIteration.isEmpty()) {
          break;
        }
        if (!waitWhileEmpty.await(stopIfEmptyForNanos, TimeUnit.NANOSECONDS)) {
          return false;
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

  /**
   * Reseeds all the PRNGs that need reseeding in {@link #byteArrayPrngsThisIteration}, then clears
   * that set.
   *
   * @return true if at least one PRNG was reseeded; false otherwise
   */
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
          byte[] seedArray = SimpleRandomSeederThread.SEED_ARRAYS
              .computeIfAbsent(random, random_ -> new byte[random_.getNewSeedLength()]);
          seedGenerator.generateSeed(seedArray);
          random.setSeed(seedArray);
        }
      }
    } finally {
      byteArrayPrngsThisIteration.clear();
    }
    return entropyConsumed;
  }

  /**
   * Generates an 8-byte seed, converts it to a long and calls {@link Random#setSeed(long)}.
   * @param random the PRNG to reseed
   */
  protected void reseedWithLong(final Random random) {
    seedGenerator.generateSeed(longSeedArray);
    random.setSeed(BinaryUtils.convertBytesToLong(longSeedArray));
  }

  /**
   * Shut down this thread even if {@link Random} instances are registered with it.
   */
  public void shutDown() {
    interrupt();
    clear();
  }

  /**
   * Removes all PRNGs from this seeder.
   */
  protected void clear() {
    lock.lock();
    try {
      unregisterWithAll(byteArrayPrngs);
      byteArrayPrngs.clear();
      byteArrayPrngsThisIteration.clear();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Informs the given PRNGs that they no longer have a seed generator. Only affects those that
   * support {@link BaseRandom#setRandomSeeder(SimpleRandomSeederThread)}.
   *
   * @param randoms the PRNGs to unregister with
   */
  protected void unregisterWithAll(Set<?> randoms) {
    synchronized (randoms) {
      randoms.forEach(random -> {
        if (random instanceof BaseRandom) {
          try {
            ((BaseRandom) random).setRandomSeeder(null);
          } catch (UnsupportedOperationException ignored) {
          }
        }
      });
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

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    initTransientFields();
  }

  /**
   * Returns the {@link SeedGenerator} this seeder uses.
   * @return this seeder's {@link SeedGenerator}
   */
  public SeedGenerator getSeedGenerator() {
    return seedGenerator;
  }

  /**
   * A {@link ThreadFactory} that sets the name and priority of the threads it creates.
   */
  public static class DefaultThreadFactory implements ThreadFactory, Serializable {

    private static final long serialVersionUID = -5806852086706570346L;
    private final String name;
    private final int priority;

    /**
     * Creates an instance with a reasonable default priority for most applications.
     *
     * @param name the name of the created thread; see {@link Thread#setName(String)}
     */
    public DefaultThreadFactory(String name) {
      this(name, Thread.NORM_PRIORITY + 1);
    }

    /**
     * Creates an instance.
     *
     * @param name the name of the created thread; see {@link Thread#setName(String)}
     * @param priority the priority of the created thread; see {@link Thread#setPriority(int)}
     */
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
}
