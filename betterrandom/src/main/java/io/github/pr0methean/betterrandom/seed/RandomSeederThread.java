package io.github.pr0methean.betterrandom.seed;

import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import io.github.pr0methean.betterrandom.EntropyCountingRandom;
import io.github.pr0methean.betterrandom.util.LogPreFormatter;
import io.github.pr0methean.betterrandom.util.LooperThread;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.logging.Level;

/**
 * Thread that loops over {@link Random} instances and reseeds them. No {@link
 * EntropyCountingRandom} will be reseeded when it's already had more input than output.
 * @author Chris Hennick
 */
@SuppressWarnings("ClassExplicitlyExtendsThread")
public final class RandomSeederThread extends LooperThread {

  private static final ExecutorService WAKER_UPPER = Executors.newSingleThreadExecutor();
  private static final LogPreFormatter LOG = new LogPreFormatter(RandomSeederThread.class);
  @SuppressWarnings("StaticCollection") private static final Map<SeedGenerator, RandomSeederThread>
      INSTANCES =
      Collections.synchronizedMap(new WeakHashMap<SeedGenerator, RandomSeederThread>(1));
  private static final long serialVersionUID = 5229976461051217528L;
  private static final long POLL_INTERVAL = 60;
  private final SeedGenerator seedGenerator;
  private final byte[] longSeedArray = new byte[8];
  // WeakHashMap-based Set can't be serialized, so read & write this copy instead
  private final Set<Random> prngsSerial = new HashSet<>();
  private transient Set<Random> prngs;
  private transient ByteBuffer longSeedBuffer;
  private transient Condition waitWhileEmpty;
  private transient Condition waitForEntropyDrain;
  private transient Set<Random> prngsThisIteration;
  private transient WeakHashMap<ByteArrayReseedableRandom, byte[]> seedArrays;

  public RandomSeederThread(final ThreadGroup group, final Runnable target, final String name,
      final long stackSize, final SeedGenerator seedGenerator) {
    super(group, target, name, stackSize);
    this.seedGenerator = seedGenerator;
    initTransientFields();
  }

  /**
   * Private constructor because only one instance per seed source.
   */
  private RandomSeederThread(final SeedGenerator seedGenerator) {
    this.seedGenerator = seedGenerator;
    initTransientFields();
  }

  /**
   * Obtain the instance for the given {@link SeedGenerator}, creating and starting it if it doesn't
   * exist.
   * @param seedGenerator the {@link SeedGenerator} to use to seed PRNGs registered with this
   *     RandomSeederThread.
   * @return a RandomSeederThread that is running and is backed by {@code seedGenerator}.
   * @deprecated Relying on a specific instance causes issues if the {@link SeedGenerator} can ever
   * throw a {@link SeedException}.
   */
  @Deprecated
  public static RandomSeederThread getInstance(final SeedGenerator seedGenerator) {
    RandomSeederThread instance;
    synchronized (INSTANCES) {
      instance = INSTANCES.get(seedGenerator);
      if (instance == null) {
        LOG.info("Creating a RandomSeederThread for %s", seedGenerator);
        instance = new RandomSeederThread(seedGenerator);
        instance.setName("RandomSeederThread for " + seedGenerator);
        instance.setDaemon(true);
        instance.setPriority(Thread.MIN_PRIORITY);
        instance.start();
      }
      INSTANCES.put(seedGenerator, instance);
    }
    return instance;
  }

  /**
   * Shut down all instances with which no {@link Random} instances are registered.
   */
  public static void stopAllEmpty() {
    ArrayList<RandomSeederThread> toStop;
    do {
      toStop = new ArrayList<>();
      synchronized (INSTANCES) {
        // This method is complicated because stopIfEmpty can't be called from inside this loop due
        // to SynchronizedMap limitations.
        for (final RandomSeederThread instance : INSTANCES.values()) {
          if (instance.isEmpty()) {
            toStop.add(instance);
          }
        }
      }
      for (RandomSeederThread instance : toStop) {
        instance.stopIfEmpty();
      }
    } while (!toStop.isEmpty());
  }

  private void initTransientFields() {
    prngs =
        Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<Random, Boolean>(1)));
    longSeedBuffer = ByteBuffer.wrap(longSeedArray);
    waitWhileEmpty = lock.newCondition();
    waitForEntropyDrain = lock.newCondition();
    prngsThisIteration = new HashSet<>(1);
    seedArrays = new WeakHashMap<>(1);
  }

  @Override protected Object readResolve() {
    return getInstance(seedGenerator);
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    initTransientFields();
    if (!prngsSerial.isEmpty()) {
      synchronized (prngs) {
        prngs.addAll(prngsSerial);
      }
      lock.lock();
      try {
        waitWhileEmpty.signalAll();
      } finally {
        lock.unlock();
      }
      prngsSerial.clear();
    }
  }

  private void writeObject(final ObjectOutputStream out) throws IOException {
    synchronized (prngs) {
      prngsSerial.addAll(prngs);
    }
    out.defaultWriteObject();
    prngsSerial.clear();
  }

  /**
   * Asynchronously triggers reseeding of the given {@link EntropyCountingRandom} if it is
   * associated with a live RandomSeederThread corresponding to the given {@link SeedGenerator}.
   * @param seedGenerator the {@link SeedGenerator} that should reseed {@code random}
   * @param random a {@link Random} to be reseeded
   * @return Whether or not the reseed was successfully scheduled.
   */
  public static boolean asyncReseed(final SeedGenerator seedGenerator, final Random random) {
    synchronized (INSTANCES) {
      return getInstance(seedGenerator).asyncReseed(random);
    }
  }

  /**
   * Asynchronously triggers reseeding of the given {@link EntropyCountingRandom} if it is
   * associated with a live RandomSeederThread.
   * @param random a {@link Random} object.
   * @return Whether or not the reseed was successfully scheduled.
   * @deprecated Causes weird race conditions
   */
  @Deprecated
  @SuppressWarnings("UnusedReturnValue") public boolean asyncReseed(final Random random) {
    if (!(random instanceof EntropyCountingRandom)) {
      // Reseed of non-entropy-counting Random happens every iteration anyway
      return prngs.contains(random);
    }
    final boolean eligible;
    synchronized (prngs) {
      eligible = prngs.contains(random);
    }
    if (eligible) {
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
      return true;
    } else {
      return false;
    }
  }

  @SuppressWarnings({"InfiniteLoopStatement", "ObjectAllocationInLoop", "AwaitNotInLoop"}) @Override
  public boolean iterate() throws InterruptedException {
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
        LOG.error("%s", t);
        LOG.logStackTrace(Level.SEVERE, t.getStackTrace());
        interrupt();
        return false;
      }
    }
    if (!entropyConsumed) {
      waitForEntropyDrain.await(POLL_INTERVAL, TimeUnit.SECONDS);
    }
    return true;
  }

  @Override public void interrupt() {
    synchronized (INSTANCES) {
      // Ensure dying instance is unregistered
      if (INSTANCES.get(seedGenerator) == this) {
        INSTANCES.remove(seedGenerator);
      }
    }
    super.interrupt();
  }

  /**
   * Returns true if no {@link Random} instances are registered with this RandomSeederThread.
   * @return true if no {@link Random} instances are registered with this RandomSeederThread.
   */
  public boolean isEmpty() {
    synchronized (prngs) {
      return prngs.isEmpty();
    }
  }

  /**
   * Add one or more {@link Random} instances to the thread for the given {@link SeedGenerator}.
   * @param seedGenerator The {@link SeedGenerator} that will reseed the {@code randoms}
   * @param randoms One or more {@link Random} instances to be reseeded
   */
  public static void add(SeedGenerator seedGenerator, final Random... randoms) {
    synchronized (INSTANCES) {
      getInstance(seedGenerator).add(randoms);
    }
  }

  /**
   * Add one or more {@link Random} instances. The caller must not hold locks on any of these
   * instances that are also acquired during {@link Random#setSeed(long)} or {@link
   * ByteArrayReseedableRandom#setSeed(byte[])}, as one of those methods may be called immediately
   * and this would cause a circular deadlock.
   * @param randoms One or more {@link Random} instances to be reseeded.
   * @deprecated Causes weird race conditions.
   */
  @Deprecated
  public void add(final Random... randoms) {
    lock.lock();
    if (getState() == State.TERMINATED || isInterrupted()) {
      throw new IllegalStateException("Already shut down");
    }
    Collections.addAll(prngs, randoms);
    try {
      waitForEntropyDrain.signalAll();
      waitWhileEmpty.signalAll();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Remove one or more {@link Random} instances from the thread for the given {@link SeedGenerator}
   * if such a thread exists and contains them.
   * @param seedGenerator The {@link SeedGenerator} that will reseed the {@code randoms}
   * @param randoms One or more {@link Random} instances to be reseeded
   */
  public static void remove(SeedGenerator seedGenerator, final Random... randoms) {
    synchronized (INSTANCES) {
      RandomSeederThread thread = INSTANCES.get(seedGenerator);
      if (thread != null) {
        thread.remove(randoms);
      }
    }
  }

  /**
   * Remove one or more {@link Random} instances. If this is called while {@link #getState()} ==
   * {@link State#RUNNABLE}, they may still be reseeded once more.
   * @param randoms the {@link Random} instances to remove.
   * @deprecated Causes weird race conditions.
   */
  @Deprecated
  public void remove(final Random... randoms) {
    prngs.removeAll(Arrays.asList(randoms));
  }

  /**
   * Shut down this thread if no {@link Random} instances are registered with it.
   */
  public void stopIfEmpty() {
    lock.lock();
    try {
      if (isEmpty()) {
        LOG.info("Stopping empty RandomSeederThread for %s", seedGenerator);
        interrupt();
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Use this to transition old methods away from the deprecated instance methods.
   * @return the {@link SeedGenerator} that this thread uses
   */
  public SeedGenerator getSeedGenerator() {
    return seedGenerator;
  }
}
