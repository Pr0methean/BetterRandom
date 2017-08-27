package betterrandom.prng.adapter;

import betterrandom.seed.DefaultSeedGenerator;
import betterrandom.seed.RandomSeederThread;
import betterrandom.seed.SeedException;
import betterrandom.seed.SeedGenerator;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

/**
 * A version of {@link SplittableRandomAdapter} that uses a {@link RandomSeederThread} to replace
 * each thread's {@link SplittableRandom} with a reseeded one as frequently as possible, but not
 * more frequently than it is being used.
 */
public class ReseedingSplittableRandomAdapter extends BaseSplittableRandomAdapter {

  private static final long serialVersionUID = 6301096404034224037L;
  private static final Map<SeedGenerator, ReseedingSplittableRandomAdapter> INSTANCES =
      Collections.synchronizedMap(new WeakHashMap<>());
  private static final Lock defaultInstanceLock = new ReentrantLock();
  @Nullable
  private static ReseedingSplittableRandomAdapter defaultInstance;
  protected final SeedGenerator seedGenerator;
  @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
  private RandomSeederThread seederThread;
  @SuppressWarnings({"ThreadLocalNotStaticFinal",
      "InstanceVariableMayNotBeInitializedByReadObject"})
  private transient ThreadLocal<SingleThreadSplittableRandomAdapter> threadLocal;

  /**
   * Single instance per SeedGenerator.
   *
   * @param seedGenerator The seed generator this adapter will use.
   */
  private ReseedingSplittableRandomAdapter(SeedGenerator seedGenerator) throws SeedException {
    super(seedGenerator.generateSeed(SEED_LENGTH_BYTES));
    this.seedGenerator = seedGenerator;
    initSubclassTransientFields();
  }

  @SuppressWarnings("NonThreadSafeLazyInitialization")
  public static ReseedingSplittableRandomAdapter getDefaultInstance()
      throws SeedException {
    defaultInstanceLock.lock();
    try {
      if (defaultInstance == null) {
        defaultInstance = new ReseedingSplittableRandomAdapter(
            DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
      }
      return defaultInstance;
    } finally {
      defaultInstanceLock.unlock();
    }
  }

  @SuppressWarnings("SynchronizationOnStaticField")
  public static ReseedingSplittableRandomAdapter getInstance(SeedGenerator seedGenerator)
      throws SeedException {
    if (seedGenerator.equals(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR)) {
      return getDefaultInstance();
    }
    synchronized (INSTANCES) {
      try {
        return INSTANCES.computeIfAbsent(seedGenerator, seedGenerator1 -> {
          try {
            return new ReseedingSplittableRandomAdapter(seedGenerator);
          } catch (SeedException e) {
            throw new RuntimeException(e);
          }
        });
      } catch (RuntimeException e) {
        Throwable cause = e.getCause();
        if (cause instanceof SeedException) {
          throw (SeedException) cause;
        } else {
          throw e;
        }
      }
    }
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    checkReadObject(in);
    assert seedGenerator != null : "@AssumeAssertion(nullness)";
    initSubclassTransientFields();
  }

  private ReseedingSplittableRandomAdapter readResolve() throws IOException {
    try {
      return getInstance(seedGenerator);
    } catch (SeedException e) {
      throw new IOException(e);
    }
  }

  @EnsuresNonNull({"threadLocal", "seederThread"})
  @RequiresNonNull("seedGenerator")
  private void initSubclassTransientFields(
      @UnknownInitialization(BaseSplittableRandomAdapter.class)ReseedingSplittableRandomAdapter this) {
    if (threadLocal == null) {
      threadLocal = ThreadLocal.withInitial(() -> {
        try {
          return new SingleThreadSplittableRandomAdapter(seedGenerator);
        } catch (SeedException e) {
          throw new RuntimeException(e);
        }
      });
    }
    seederThread = RandomSeederThread.getInstance(seedGenerator);
  }

  @Override
  protected SplittableRandom getSplittableRandom() {
    SingleThreadSplittableRandomAdapter adapterForThread = threadLocal.get();
    seederThread.add(adapterForThread);
    return adapterForThread.getSplittableRandom();
  }

  @Override
  protected void finalize() {
    seederThread.stopIfEmpty();
  }

  @Override
  public boolean equals(Object o) {
    return this == o
        || (o instanceof ReseedingSplittableRandomAdapter
        && seedGenerator.equals(((ReseedingSplittableRandomAdapter) o).seedGenerator));
  }

  @Override
  public int hashCode() {
    return seedGenerator.hashCode() + 1;
  }

  @Override
  public String toString() {
    return "ReseedingSplittableRandomAdapter using " + seedGenerator;
  }
}
