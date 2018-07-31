package io.github.pr0methean.betterrandom.seed;

import com.google.common.cache.CacheBuilder;
import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import io.github.pr0methean.betterrandom.EntropyCountingRandom;
import io.github.pr0methean.betterrandom.util.LooperThread;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

  private static final ExecutorService WAKER_UPPER = Executors.newSingleThreadExecutor();
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
  private final ByteBuffer longSeedBuffer = ByteBuffer.wrap(longSeedArray);
  private final Set<ByteArrayReseedableRandom> byteArrayPrngsThisIteration = new HashSet<>(1);
  private final Set<Random> otherPrngsThisIteration = new HashSet<>(1);
  private final WeakHashMap<ByteArrayReseedableRandom, byte[]> seedArrays = new WeakHashMap<>(1);
  private static final AtomicInteger defaultPriority = new AtomicInteger(Thread.NORM_PRIORITY);

  /**
   * Private constructor because only one instance per seed source.
   */
  private RandomSeederThread(final SeedGenerator seedGenerator) {
    this.seedGenerator = seedGenerator;
  }

  /**
   * Obtain the instance for the given {@link SeedGenerator}, creating and starting it if it doesn't
   * exist.
   * @param seedGenerator the {@link SeedGenerator} to use to seed PRNGs registered with this
   *     RandomSeederThread.
   * @return a RandomSeederThread that is running and is backed by {@code seedGenerator}.
   */
  private static RandomSeederThread getInstance(final SeedGenerator seedGenerator) {
    return INSTANCES.computeIfAbsent(seedGenerator, seedGen -> {
      System.out.format("Creating a RandomSeederThread for %s%n", seedGen);
      final RandomSeederThread thread = new RandomSeederThread(seedGen);
      thread.setName("RandomSeederThread for " + seedGen);
      thread.setDaemon(true);
      thread.setPriority(defaultPriority.get());
      thread.start();
      return thread;
    });
  }

  /**
   * Returns whether a RandomSeederThread using the given {@link SeedGenerator} is running or not.
   * @param seedGenerator a {@link SeedGenerator} to find an instance for.
   * @return true if a RandomSeederThread using the given {@link SeedGenerator} is running; false
   *     otherwise.
   */
  public static boolean hasInstance(final SeedGenerator seedGenerator) {
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
   * Asynchronously triggers reseeding of the given {@link EntropyCountingRandom} if it is
   * associated with a live RandomSeederThread corresponding to the given {@link SeedGenerator}.
   * @param seedGenerator the {@link SeedGenerator} that should reseed {@code random}
   * @param random a {@link Random} to be reseeded
   * @return Whether or not the reseed was successfully scheduled.
   */
  public static boolean asyncReseed(final SeedGenerator seedGenerator, final Random random) {
    final RandomSeederThread thread = getInstance(seedGenerator);
    return thread != null && thread.asyncReseed(random);
  }

  public static boolean isEmpty(final SeedGenerator seedGenerator) {
    final RandomSeederThread thread = getInstance(seedGenerator);
    return thread == null || thread.isEmpty();
  }

  /**
   * Add one or more {@link Random} instances to the thread for the given {@link SeedGenerator}.
   * @param seedGenerator The {@link SeedGenerator} that will reseed the {@code randoms}
   * @param randoms One or more {@link Random} instances to be reseeded
   */
  public static void add(final SeedGenerator seedGenerator, final Random... randoms) {
    boolean notSucceeded = true;
    do {
      try {
        getInstance(seedGenerator).add(randoms);
        notSucceeded = false;
      } catch (IllegalStateException ignored) {
        // Get the new instance and try again.
      }
    } while (notSucceeded);
  }

  /**
   * Remove one or more {@link Random} instances from the thread for the given {@link SeedGenerator}
   * if such a thread exists and contains them.
   * @param seedGenerator The {@link SeedGenerator} that will reseed the {@code randoms}
   * @param randoms One or more {@link Random} instances to be reseeded
   */
  public static void remove(final SeedGenerator seedGenerator, final Random... randoms) {
    final RandomSeederThread thread = INSTANCES.get(seedGenerator);
    if (thread != null) {
      thread.remove(randoms);
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
   * Sets the priority of a random-seeder thread, starting it if it's not already running.
   * @param seedGenerator the {@link SeedGenerator} of the thread whose priority should change
   * @param priority the thread priority
   * @see Thread#setPriority(int)
   */
  public static void setPriority(final SeedGenerator seedGenerator, int priority) {
    getInstance(seedGenerator).setPriority(priority);
  }

  public static void stopIfEmpty(final SeedGenerator seedGenerator) {
    final RandomSeederThread thread = INSTANCES.get(seedGenerator);
    if (thread != null) {
      thread.stopIfEmpty();
    }
  }

  /**
   * Asynchronously triggers reseeding of the given {@link EntropyCountingRandom} if it is
   * associated with a live RandomSeederThread.
   * @param random a {@link Random} object.
   * @return Whether or not the reseed was successfully scheduled.
   */
  private boolean asyncReseed(final Random random) {
    if (!isAlive() || !otherPrngs.contains(random)) {
      return false;
    }
    if (random instanceof EntropyCountingRandom) {
      // Reseed of non-entropy-counting Random happens every iteration anyway
      WAKER_UPPER.submit(() -> {
        lock.lock();
        try {
          waitForEntropyDrain.signalAll();
        } finally {
          lock.unlock();
        }
      });
    }
    return true;
  }

  @SuppressWarnings({"InfiniteLoopStatement", "ObjectAllocationInLoop", "AwaitNotInLoop"}) @Override
  protected boolean iterate() throws InterruptedException {
    System.out.format("Thread for %s: iterate() called%n", seedGenerator);
    while (true) {
      otherPrngsThisIteration.addAll(otherPrngs);
      byteArrayPrngsThisIteration.addAll(byteArrayPrngs);
      if (otherPrngsThisIteration.isEmpty() && byteArrayPrngsThisIteration.isEmpty()) {
        System.out.format("Thread for %s is empty%n", seedGenerator);
        waitWhileEmpty.await();
      } else {
        break;
      }
    }
    boolean entropyConsumed = false;
    try {
      final Iterator<ByteArrayReseedableRandom> byteArrayPrngsIterator =
          byteArrayPrngsThisIteration.iterator();
      while (byteArrayPrngsIterator.hasNext()) {
        final ByteArrayReseedableRandom random = byteArrayPrngsIterator.next();
        byteArrayPrngsIterator.remove();
        if (stillDefinitelyHasEntropy(random)) {
          System.out.format("Thread for %s: skipping %s (still has entropy)%n", seedGenerator, random);
          continue;
        }
        entropyConsumed = true;
        if (random.preferSeedWithLong()) {
          reseedWithLong((Random) random);
        } else {
          System.out.format("Thread for %s: generating byte[] seed for %s%n", seedGenerator, random);
          final byte[] seedArray =
              seedArrays.computeIfAbsent(random, random_ -> new byte[random_.getNewSeedLength()]);
          seedGenerator.generateSeed(seedArray);
          System.out.format("Thread for %s: reseeding %s%n", seedGenerator, random);
          random.setSeed(seedArray);
          System.out.format("Thread for %s: done reseeding %s%n", seedGenerator, random);
        }
      }
      final Iterator<Random> otherPrngsIterator = otherPrngsThisIteration.iterator();
      while (otherPrngsIterator.hasNext()) {
        final Random random = otherPrngsIterator.next();
        otherPrngsIterator.remove();
        if (stillDefinitelyHasEntropy(random)) {
          System.out.format("Thread for %s: skipping %s (still has entropy)%n", seedGenerator, random);
          continue;
        }
        entropyConsumed = true;
        reseedWithLong(random);
      }
    } catch (final Throwable t) {
      // Must unlock before interrupt; otherwise we somehow get a deadlock
      lock.unlock();
      LOG.error("Error during reseeding; disabling the RandomSeederThread for " + seedGenerator, t);
      interrupt();
      // Must lock again before returning, so we can notify conditions
      lock.lock();
      return false;
    }
    if (!entropyConsumed) {
      System.out.format("Thread for %s: sleeping because no entropy was consumed%n", seedGenerator);
      waitForEntropyDrain.await(POLL_INTERVAL, TimeUnit.SECONDS);
    }
    System.out.format("Thread for %s: iterate() returning%n", seedGenerator);
    return true;
  }

  private void reseedWithLong(Random random) {
    System.out.format("Thread for %s: generating long seed for %s%n", seedGenerator, random);
    seedGenerator.generateSeed(longSeedArray);
    System.out.format("Thread for %s: reseeding %s%n", seedGenerator, random);
    random.setSeed(longSeedBuffer.getLong(0));
    System.out.format("Thread for %s: done reseeding %s%n", seedGenerator, random);
  }

  private static boolean stillDefinitelyHasEntropy(Object random) {
    return (random instanceof EntropyCountingRandom) &&
        (((EntropyCountingRandom) random).getEntropyBits() > 0);
  }

  @Override public void interrupt() {
    // Ensure dying instance is unregistered
    INSTANCES.remove(seedGenerator, this);
    super.interrupt();
    lock.lock();
    try {
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
   * Add one or more {@link Random} instances. The caller must not hold locks on any of these
   * instances that are also acquired during {@link Random#setSeed(long)} or {@link
   * ByteArrayReseedableRandom#setSeed(byte[])}, as one of those methods may be called immediately
   * and this would cause a circular deadlock.
   * @param randoms One or more {@link Random} instances to be reseeded.
   */
  private void add(final Random... randoms) {
    System.out.format("Thread for %s: add() called%n", seedGenerator);
    lock.lock();
    try {
      System.out.format("Thread for %s: lock obtained for add()%n", seedGenerator);
      if ((getState() == State.TERMINATED) || isInterrupted()) {
        throw new IllegalStateException("Already shut down");
      }
      for (Random random : randoms) {
        if (random instanceof ByteArrayReseedableRandom) {
          byteArrayPrngs.add((ByteArrayReseedableRandom) random);
        } else {
          otherPrngs.add(random);
        }
        System.out.format("Thread for %s: added %s%n", seedGenerator, random);
      }
      waitForEntropyDrain.signalAll();
      waitWhileEmpty.signalAll();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Remove one or more {@link Random} instances. If this is called while {@link #getState()} ==
   * {@link State#RUNNABLE}, they may still be reseeded once more.
   * @param randoms the {@link Random} instances to remove.
   */
  private void remove(final Random... randoms) {
    final List<Random> randomsList = Arrays.asList(randoms);
    byteArrayPrngs.removeAll(randomsList);
    otherPrngs.removeAll(randomsList);
  }

  /**
   * Shut down this thread if no {@link Random} instances are registered with it.
   */
  private void stopIfEmpty() {
    lock.lock();
    try {
      if (isEmpty()) {
        System.out.format("Stopping empty RandomSeederThread for {}%n", seedGenerator);
        interrupt();
      }
    } finally {
      lock.unlock();
    }
  }
}
