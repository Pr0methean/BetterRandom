package io.github.pr0methean.betterrandom.seed;

import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import io.github.pr0methean.betterrandom.EntropyCountingRandom;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.Looper;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
public class RandomSeeder extends Looper {

  /**
   * Time in seconds to wait before checking again whether any PRNGs need more entropy.
   */
  protected static final long POLL_INTERVAL = 10;
  private static final long serialVersionUID = -4339570810679373476L;

  /**
   * Default waiting time before an empty instance terminates if still empty.
   */
  protected static final long DEFAULT_STOP_IF_EMPTY_FOR_NANOS = 5_000_000_000L;

  /**
   * The seed generator this seeder uses.
   */
  protected final SeedGenerator seedGenerator;

  /**
   * Holds {@link ByteArrayReseedableRandom} instances that should be reseeded when their entropy is
   * low, or as often as possible if they don't implement {@link EntropyCountingRandom}.
   */
  protected transient Set<ByteArrayReseedableRandom> byteArrayPrngs;

  /**
   * Signaled when a PRNG is added.
   */
  protected transient Condition waitWhileEmpty;

  /**
   * Signaled by an associated {@link BaseRandom} when it runs out of entropy.
   */
  protected transient Condition waitForEntropyDrain;

  /**
   * Time in nanoseconds after which this thread will terminate if no PRNGs are attached.
   */
  protected final long stopIfEmptyForNanos;

  /**
   * Creates an instance whose thread will terminate if no PRNGs have been associated with it for 5
   * seconds.
   *
   * @param seedGenerator the seed generator
   * @param threadFactory the {@link ThreadFactory} that will create this seeder's thread
   */
  public RandomSeeder(final SeedGenerator seedGenerator, ThreadFactory threadFactory) {
    this(seedGenerator, threadFactory, DEFAULT_STOP_IF_EMPTY_FOR_NANOS);
  }

  /**
   * Creates an instance using a {@link DefaultThreadFactory}.
   *
   * @param seedGenerator the seed generator
   */
  public RandomSeeder(SeedGenerator seedGenerator) {
    this(seedGenerator, new RandomSeeder.DefaultThreadFactory(seedGenerator.toString()));
  }

  /**
   * Creates an instance.
   *
   * @param seedGenerator the seed generator
   * @param threadFactory the {@link ThreadFactory} that will create this seeder's thread
   * @param stopIfEmptyForNanos time in nanoseconds after which this thread will terminate if no
   *     PRNGs are attached
   */
  public RandomSeeder(SeedGenerator seedGenerator, ThreadFactory threadFactory,
      long stopIfEmptyForNanos) {
    super(threadFactory);
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
  public void remove(Object... randoms) {
    remove(Arrays.asList(randoms));
  }

  /**
   * Removes PRNGs so that they will no longer be reseeded.
   * @param randoms the PRNGs to remove
   */
  public void remove(Collection<?> randoms) {
    if (randoms.isEmpty()) {
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
   * Checks whether the given PRNG is currently registered with this RandomSeeder.
   *
   * @param random the PRNG to check the status of
   * @return true if registered; false if not
   */
  public boolean contains(Object random) {
    return byteArrayPrngs.contains(random);
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
    if (randoms.isEmpty()) {
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

  /**
   * Ensures this seeder's thread is started, and signals conditions it may be waiting on.
   */
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

  /**
   * Returns the {@link Logger} for this class.
   * @return the logger for this class
   */
  private static Logger getLogger() {
    return LoggerFactory.getLogger(RandomSeeder.class);
  }

  @Override public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RandomSeeder that = (RandomSeeder) o;
    return seedGenerator.equals(that.seedGenerator) && factory.equals(that.factory);
  }

  @Override public int hashCode() {
    return 31 * seedGenerator.hashCode() + factory.hashCode();
  }

  /**
   * Initializes the transient instance fields for this class. Called by constructors and during
   * deserialization.
   */
  protected void initTransientFields() {
    byteArrayPrngs = createSynchronizedWeakHashSet();
    waitWhileEmpty = lock.newCondition();
    waitForEntropyDrain = lock.newCondition();
  }

  /**
   * Creates and returns a thread-safe {@link Set} with only weak references to its members.
   *
   * @param <T> the set element type
   * @return an empty mutable thread-safe {@link Set} that holds only weak references to its members
   */
  protected <T> Set<T> createSynchronizedWeakHashSet() {
    return Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<T, Boolean>(1)));
  }

  @SuppressWarnings({"InfiniteLoopStatement", "ObjectAllocationInLoop", "AwaitNotInLoop"}) @Override
  protected boolean iterate() {
    Collection<ByteArrayReseedableRandom> byteArrayPrngsThisIteration = new ArrayList<>(byteArrayPrngs);
    try {
      while (byteArrayPrngsThisIteration.isEmpty()) {
        if (!waitWhileEmpty.await(stopIfEmptyForNanos, TimeUnit.NANOSECONDS)) {
          return false;
        }
        byteArrayPrngsThisIteration.addAll(byteArrayPrngs);
      }
      boolean entropyConsumed = reseedByteArrayReseedableRandoms(byteArrayPrngsThisIteration);
      if (!entropyConsumed) {
        waitForEntropyDrain.await(POLL_INTERVAL, TimeUnit.SECONDS);
      }
      return true;
    } catch (final Throwable t) {
      getLogger().error("Disabling the LegacyRandomSeeder for " + seedGenerator, t);
      return false;
    }
  }

  /**
   * Reseeds all the PRNGs that need reseeding in {@link #randoms}.
   *
   * @return true if at least one PRNG was reseeded; false otherwise
   */
  protected boolean reseedByteArrayReseedableRandoms(Iterable<? extends ByteArrayReseedableRandom> randoms) {
    boolean entropyConsumed = false;
    for (ByteArrayReseedableRandom random : randoms) {
      if (stillDefinitelyHasEntropy(random)) {
        continue;
      }
      entropyConsumed = true;
      if (random.preferSeedWithLong()) {
        reseedWithLong((Random) random);
      } else {
        byte[] seed = seedGenerator.generateSeed(random.getNewSeedLength());
        random.setSeed(seed);
      }
    }
    return entropyConsumed;
  }

  /**
   * Generates an 8-byte seed, converts it to a long and calls {@link Random#setSeed(long)}.
   * @param random the PRNG to reseed
   */
  protected void reseedWithLong(final Random random) {
    random.setSeed(BinaryUtils.convertBytesToLong(seedGenerator.generateSeed(Long.BYTES)));
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
    } finally {
      lock.unlock();
    }
  }

  /**
   * Informs the given PRNGs that they no longer have a seed generator. Only affects those that
   * support {@link BaseRandom#setRandomSeeder(RandomSeeder)}.
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
   * Returns true if no {@link Random} instances are registered with this LegacyRandomSeeder.
   *
   * @return true if no {@link Random} instances are registered with this LegacyRandomSeeder.
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
    wakeUp();
    lock.lock();
    try {
      if (isEmpty()) {
        getLogger().info("Stopping empty LegacyRandomSeeder for {}", seedGenerator);
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
