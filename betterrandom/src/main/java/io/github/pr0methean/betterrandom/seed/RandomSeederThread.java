package io.github.pr0methean.betterrandom.seed;

import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import io.github.pr0methean.betterrandom.EntropyCountingRandom;
import io.github.pr0methean.betterrandom.util.LooperThread;
import java.nio.ByteBuffer;
import java.util.ArrayList;
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
      INSTANCES =
      Collections.synchronizedMap(new WeakHashMap<SeedGenerator, RandomSeederThread>(1));
  private static final long POLL_INTERVAL = 60;
  private final SeedGenerator seedGenerator;
  private final Condition waitWhileEmpty = lock.newCondition();
  private final Condition waitForEntropyDrain = lock.newCondition();
  private final Set<Random> prngs
      = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<Random, Boolean>(1)));
  private final byte[] longSeedArray = new byte[8];
  private final ByteBuffer longSeedBuffer = ByteBuffer.wrap(longSeedArray);
  private final Set<Random> prngsThisIteration = new HashSet<>(1);
  private final WeakHashMap<ByteArrayReseedableRandom, byte[]> seedArrays
       = new WeakHashMap<>(1);
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
    RandomSeederThread instance;
    instance = INSTANCES.get(seedGenerator);
    if (instance == null) {
      LOG.info("Creating a RandomSeederThread for {}", seedGenerator);
      instance = new RandomSeederThread(seedGenerator);
      instance.setName("RandomSeederThread for " + seedGenerator);
      instance.setDaemon(true);
      instance.setPriority(defaultPriority.get());
      instance.start();
    }
    if (!INSTANCES.replace(seedGenerator, null, instance)) {
      instance.interrupt();
    }
    return instance;
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
    RandomSeederThread thread = getInstance(seedGenerator);
    return thread != null && thread.asyncReseed(random);
  }

  public static boolean isEmpty(final SeedGenerator seedGenerator) {
    return (!hasInstance(seedGenerator)) || getInstance(seedGenerator).isEmpty();
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
    if (!isAlive() || !prngs.contains(random)) {
      return false;
    }
    if (random instanceof EntropyCountingRandom) {
      // Reseed of non-entropy-counting Random happens every iteration anyway
      WAKER_UPPER.submit(new Runnable() {
        @Override public void run() {
          lock.lock();
          try {
            waitForEntropyDrain.signalAll();
          } finally {
            lock.unlock();
          }
        }
      });
    }
    return true;
  }

  @SuppressWarnings({"InfiniteLoopStatement", "ObjectAllocationInLoop", "AwaitNotInLoop"}) @Override
  protected boolean iterate() throws InterruptedException {
    while (true) {
      synchronized (prngs) {
        prngsThisIteration.addAll(prngs);
      }
      if (prngsThisIteration.isEmpty()) {
        waitWhileEmpty.await();
      } else {
        break;
      }
    }
    final Iterator<Random> iterator = prngsThisIteration.iterator();
    boolean entropyConsumed = false;
    while (iterator.hasNext()) {
      final Random random = iterator.next();
      iterator.remove();
      if ((random instanceof EntropyCountingRandom) && (
          ((EntropyCountingRandom) random).getEntropyBits() > 0)) {
        continue;
      } else {
        entropyConsumed = true;
      }
      try {
        if ((random instanceof ByteArrayReseedableRandom) && !((ByteArrayReseedableRandom) random)
            .preferSeedWithLong()) {
          final ByteArrayReseedableRandom reseedable = (ByteArrayReseedableRandom) random;
          byte[] seedArray = seedArrays.get(reseedable);
          if (seedArray == null) {
            seedArray = new byte[reseedable.getNewSeedLength()];
            seedArrays.put(reseedable, seedArray);
          }
          seedGenerator.generateSeed(seedArray);
          reseedable.setSeed(seedArray);
        } else {
          seedGenerator.generateSeed(longSeedArray);
          random.setSeed(longSeedBuffer.getLong(0));
        }
      } catch (final Throwable t) {
        // Must unlock before interrupt; otherwise we somehow get a deadlock
        lock.unlock();
        LOG.error("Error during reseeding; disabling the RandomSeederThread for " + seedGenerator,
            t);
        interrupt();
        // Must lock again before returning, so we can notify conditions
        lock.lock();
        return false;
      }
    }
    if (!entropyConsumed) {
      waitForEntropyDrain.await(POLL_INTERVAL, TimeUnit.SECONDS);
    }
    return true;
  }

  @Override public void interrupt() {
    // Ensure dying instance is unregistered
    INSTANCES.remove(seedGenerator, this);
    super.interrupt();
    lock.lock();
    try {
      prngs.clear();
      prngsThisIteration.clear();
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
    synchronized (prngs) {
      return prngs.isEmpty();
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
    lock.lock();
    try {
      if ((getState() == State.TERMINATED) || isInterrupted()) {
        throw new IllegalStateException("Already shut down");
      }
      Collections.addAll(prngs, randoms);
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
    prngs.removeAll(Arrays.asList(randoms));
  }

  /**
   * Shut down this thread if no {@link Random} instances are registered with it.
   */
  private void stopIfEmpty() {
    lock.lock();
    try {
      if (isEmpty()) {
        LOG.info("Stopping empty RandomSeederThread for {}", seedGenerator);
        interrupt();
      }
    } finally {
      lock.unlock();
    }
  }
}
