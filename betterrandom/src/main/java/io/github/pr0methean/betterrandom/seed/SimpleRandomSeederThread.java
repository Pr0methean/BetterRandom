package io.github.pr0methean.betterrandom.seed;

import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import io.github.pr0methean.betterrandom.EntropyCountingRandom;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.LooperThread;
import java.util.Arrays;
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

public abstract class SimpleRandomSeederThread extends LooperThread {
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
  }

  static boolean stillDefinitelyHasEntropy(final Object random) {
    if (!(random instanceof EntropyCountingRandom)) {
      return false;
    }
    EntropyCountingRandom entropyCountingRandom = (EntropyCountingRandom) random;
    return !entropyCountingRandom.needsReseedingEarly() &&
        entropyCountingRandom.getEntropyBits() > 0;
  }

  public void remove(Random... randoms) {
    if (randoms.length == 0) {
      return;
    }
    lock.lock();
    try {
      byteArrayPrngs.removeAll(Arrays.asList(randoms));
    } finally {
      lock.unlock();
    }
  }

  public void add(ByteArrayReseedableRandom... randoms) {
    if (randoms.length == 0) {
      return;
    }
    lock.lock();
    try {
      byteArrayPrngs.addAll(Arrays.asList(randoms));
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

  @SuppressWarnings({"InfiniteLoopStatement", "ObjectAllocationInLoop", "AwaitNotInLoop"}) @Override
  protected boolean iterate() {
    try {
      while (true) {
        byteArrayPrngsThisIteration.addAll(byteArrayPrngs);
        if (!byteArrayPrngsThisIteration.isEmpty()) {
          break;
        }
      }
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
            final byte[] seedArray = RandomSeederThread.SEED_ARRAYS
                .computeIfAbsent(random, random_ -> new byte[random_.getNewSeedLength()]);
            seedGenerator.generateSeed(seedArray);
            random.setSeed(seedArray);
          }
        }
      } finally {
        byteArrayPrngsThisIteration.clear();
      }
      if (!entropyConsumed) {
        waitForEntropyDrain.await(POLL_INTERVAL, TimeUnit.SECONDS);
      }
      return true;
    } catch (final Throwable t) {
      getLogger().error("Disabling the RandomSeederThread for " + seedGenerator, t);
      return false;
    }
  }

  protected void reseedWithLong(final Random random) {
    seedGenerator.generateSeed(longSeedArray);
    random.setSeed(BinaryUtils.convertBytesToLong(longSeedArray));
  }
}
