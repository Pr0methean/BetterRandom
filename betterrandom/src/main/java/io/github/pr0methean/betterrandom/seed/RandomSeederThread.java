package io.github.pr0methean.betterrandom.seed;

import com.google.common.cache.CacheBuilder;
import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import io.github.pr0methean.betterrandom.EntropyCountingRandom;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.LooperThread;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread that loops over {@link Random} instances and reseeds them. No {@link
 * EntropyCountingRandom} will be reseeded when it's already had more input than output.
 * @author Chris Hennick
 */
@SuppressWarnings("ClassExplicitlyExtendsThread")
public final class RandomSeederThread extends LooperThread {

  /**
   * The initial default priority for new seeder threads.
   */
  public static final int DEFAULT_DEFAULT_PRIORITY = Thread.NORM_PRIORITY + 1;

  private static final Logger LOG = LoggerFactory.getLogger(RandomSeederThread.class);
  @SuppressWarnings("StaticCollection") private static final Map<SeedGenerator, RandomSeederThread>
      INSTANCES = new ConcurrentHashMap<>(1);
  private static final long POLL_INTERVAL = 60;
  private final SeedGenerator seedGenerator;
  private final Condition waitWhileEmpty = lock.newCondition();
  private final Condition waitForEntropyDrain = lock.newCondition();
  private final Set<ByteArrayReseedableRandom> byteArrayPrngs = Collections.newSetFromMap(
      CacheBuilder.newBuilder().weakKeys().initialCapacity(1)
          .<ByteArrayReseedableRandom, Boolean>build().asMap());
  private final Set<Random> otherPrngs = Collections.newSetFromMap(
      CacheBuilder.newBuilder().weakKeys().initialCapacity(1)
          .<Random, Boolean>build().asMap());
  private final byte[] longSeedArray = new byte[8];
  private final Set<ByteArrayReseedableRandom> byteArrayPrngsThisIteration
      = Collections.newSetFromMap(new WeakHashMap<>(1));
  private final Set<Random> otherPrngsThisIteration
      = Collections.newSetFromMap(new WeakHashMap<>(1));
  private final WeakHashMap<ByteArrayReseedableRandom, byte[]> seedArrays = new WeakHashMap<>(1);
  private static final AtomicInteger defaultPriority = new AtomicInteger(DEFAULT_DEFAULT_PRIORITY);

  private RandomSeederThread(final SeedGenerator seedGenerator, ThreadFactory threadFactory) {
    super(threadFactory);
    Objects.requireNonNull(seedGenerator, "seedGenerator must not be null");
    this.seedGenerator = seedGenerator;
    start();
  }

  /**
   * Private constructor because only one instance per seed source.
   */
  private RandomSeederThread(final SeedGenerator seedGenerator) {
    this(seedGenerator, runnable -> {
      Thread thread = new Thread(runnable, "RandomSeederThread for " + seedGenerator);
      thread.setDaemon(true);
      thread.setPriority(defaultPriority.get());
      return thread;
    });
  }

  /**
   * Obtain the instance for the given {@link SeedGenerator}, creating and starting it if it doesn't
   * exist.
   * @param seedGenerator the {@link SeedGenerator} to use to seed PRNGs registered with this
   *     RandomSeederThread.
   * @return a RandomSeederThread that is running and is backed by {@code seedGenerator}.
   */
  private static RandomSeederThread getInstance(final SeedGenerator seedGenerator) {
    Objects.requireNonNull(seedGenerator, "seedGenerator must not be null");
    return INSTANCES.computeIfAbsent(seedGenerator, RandomSeederThread::new);
  }

  /**
   * Returns whether a RandomSeederThread using the given {@link SeedGenerator} is running or not.
   * @param seedGenerator a {@link SeedGenerator} to find an instance for.
   * @return true if a RandomSeederThread using the given {@link SeedGenerator} is running; false
   *     otherwise.
   */
  public static boolean hasInstance(final SeedGenerator seedGenerator) {
    Objects.requireNonNull(seedGenerator, "seedGenerator must not be null");
    return INSTANCES.containsKey(seedGenerator);
  }

  /**
   * Shut down all instances with which no {@link Random} instances are registered.
   */
  public static void stopAllEmpty() {
    final List<RandomSeederThread> toStop = new LinkedList<>(INSTANCES.values());
    for (final RandomSeederThread instance : toStop) {
      instance.stopIfEmpty();
    }
  }

  /**
   * Notifies the thread for the given {@link SeedGenerator} that PRNGs are waiting to be reseeded.
   * This method does not block, because if it cannot immediately take the lock to signal the
   * condition, then that means the thread is already running or being woken up.
   * <p>
   * Warning: This may return true during a brief window while the thread is shutting down.
   * @param seedGenerator the {@link SeedGenerator} that should reseed {@code random}
   * @return Whether or not the thread exists and is now awake.
   */
  public static boolean wakeUp(final SeedGenerator seedGenerator) {
    Objects.requireNonNull(seedGenerator, "seedGenerator must not be null");
    final RandomSeederThread thread = INSTANCES.get(seedGenerator);
    if (thread == null) {
      return false;
    }
    if (thread.lock.tryLock()) {
      try {
        thread.waitForEntropyDrain.signalAll();
      } finally {
        thread.lock.unlock();
      }
    }
    return true;
  }

  public static boolean isEmpty(final SeedGenerator seedGenerator) {
    final RandomSeederThread thread = INSTANCES.get(seedGenerator);
    return thread == null || thread.isEmpty();
  }

  /**
   * Add one or more {@link Random} instances to the thread for the given {@link SeedGenerator}.
   * @param seedGenerator The {@link SeedGenerator} that will reseed the {@code randoms}
   * @param randoms One or more {@link Random} instances to be reseeded
   */
  public static void add(final SeedGenerator seedGenerator, final Random... randoms) {
    if (randoms.length == 0) {
      return;
    }
    boolean notSucceeded = true;
    do {
      final RandomSeederThread thread = getInstance(seedGenerator);
      if (thread.isDead()) {
        continue;
      }
      thread.lock.lock();
      try {
        if (thread.isDead()) {
          continue;
        }
        for (final Random random : randoms) {
          if (random instanceof ByteArrayReseedableRandom) {
            thread.byteArrayPrngs.add((ByteArrayReseedableRandom) random);
          } else {
            thread.otherPrngs.add(random);
          }
        }
        thread.waitForEntropyDrain.signalAll();
        thread.waitWhileEmpty.signalAll();
      } finally {
        thread.lock.unlock();
      }
      notSucceeded = false;
    } while (notSucceeded);
  }

  /**
   * Remove one or more {@link Random} instances from the thread for the given {@link SeedGenerator}
   * if such a thread exists and contains them.
   * @param seedGenerator The {@link SeedGenerator} that will reseed the {@code randoms}
   * @param randoms One or more {@link Random} instances to be reseeded
   */
  public static void remove(final SeedGenerator seedGenerator, final Random... randoms) {
    if (randoms.length == 0) {
      return;
    }
    final RandomSeederThread thread = INSTANCES.get(seedGenerator);
    if (thread != null) {
      final List<Random> randomsList = Arrays.asList(randoms);
      thread.byteArrayPrngs.removeAll(randomsList);
      thread.otherPrngs.removeAll(randomsList);
    }
  }

  /**
   * Sets the default priority for new random-seeder threads.
   * @param priority the thread priority
   * @see Thread#setPriority(int)
   */
  public static void setDefaultPriority(final int priority) {
    defaultPriority.set(priority);
  }

  /**
   * Gets the default priority for new random-seeder threads.
   * @return the thread priority
   * @see Thread#setPriority(int)
   */
  public static int getDefaultPriority() {
    return defaultPriority.get();
  }

  /**
   * Sets the priority of a random-seeder thread, starting it if it's not already running.
   * @param seedGenerator the {@link SeedGenerator} of the thread whose priority should change
   * @param priority the thread priority
   * @see Thread#setPriority(int)
   */
  public static void setPriority(final SeedGenerator seedGenerator, final int priority) {
    getInstance(seedGenerator).setPriority(priority);
  }

  public static void stopIfEmpty(final SeedGenerator seedGenerator) {
    final RandomSeederThread thread = INSTANCES.get(seedGenerator);
    if (thread != null) {
      thread.stopIfEmpty();
    }
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
              seedArrays.computeIfAbsent(random, random_ -> new byte[random_.getNewSeedLength()]);
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
    Objects.requireNonNull(seedGenerator);
    INSTANCES.remove(seedGenerator, this);
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
          ((BaseRandom) random).setSeedGenerator(null);
        }
      }
      byteArrayPrngs.clear();
      byteArrayPrngsThisIteration.clear();
      otherPrngs.clear();
      otherPrngsThisIteration.clear();
      seedArrays.clear();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Removes all PRNGs from a given seed generator's thread.
   * @param seedGenerator the {@link SeedGenerator} of the thread to clear
   */
  public static void clear(final SeedGenerator seedGenerator) {
    final RandomSeederThread thread = INSTANCES.get(seedGenerator);
    if (thread != null) {
      thread.clear();
    }
  }

  /**
   * Returns true if no {@link Random} instances are registered with this RandomSeederThread.
   * @return true if no {@link Random} instances are registered with this RandomSeederThread.
   */
  private boolean isEmpty() {
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
  private void stopIfEmpty() {
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
