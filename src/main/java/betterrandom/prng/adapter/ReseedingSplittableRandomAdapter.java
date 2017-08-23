package betterrandom.prng.adapter;

import betterrandom.seed.DefaultSeedGenerator;
import betterrandom.seed.RandomSeederThread;
import betterrandom.seed.SeedException;
import betterrandom.seed.SeedGenerator;
import java.util.SplittableRandom;
import java.util.WeakHashMap;
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
  private static final WeakHashMap<SeedGenerator, ReseedingSplittableRandomAdapter> INSTANCES = new WeakHashMap<>();
  @Nullable private static ReseedingSplittableRandomAdapter defaultInstance;
  private transient RandomSeederThread seederThread; // Transient to work around Oracle bug 9050586
  private transient ThreadLocal<SingleThreadSplittableRandomAdapter> threadLocal;
  protected final SeedGenerator seedGenerator; // to initialize it early for a subclass

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

  public static synchronized ReseedingSplittableRandomAdapter getDefaultInstance()
      throws SeedException {
    if (defaultInstance == null) {
      defaultInstance = new ReseedingSplittableRandomAdapter(DefaultSeedGenerator.getInstance());
    }
    return defaultInstance;
  }

  @SuppressWarnings("contracts.postcondition.override.invalid")
  @EnsuresNonNull({"threadLocal", "seederThread"})
  @RequiresNonNull("seedGenerator")
  @Override
  protected void initSubclassTransientFields(
      @UnknownInitialization ReseedingSplittableRandomAdapter this) {
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

  public ReseedingSplittableRandomAdapter getInstance(SeedGenerator seedGenerator)
      throws SeedException {
    if (seedGenerator.equals(DefaultSeedGenerator.getInstance())) {
      return getDefaultInstance();
    }
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
}
