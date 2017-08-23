package betterrandom.prng.adapter;

import betterrandom.seed.DefaultSeedGenerator;
import betterrandom.seed.RandomSeederThread;
import betterrandom.seed.SeedException;
import betterrandom.seed.SeedGenerator;
import java.util.SplittableRandom;
import java.util.WeakHashMap;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A version of {@link SplittableRandomAdapter} that uses a {@link RandomSeederThread} to replace
 * each thread's {@link SplittableRandom} with a reseeded one as frequently as possible, but not
 * more frequently than it is being used.
 */
public class ReseedingSplittableRandomAdapter extends SplittableRandomAdapter {

  private static final long serialVersionUID = 6301096404034224037L;
  private static final WeakHashMap<SeedGenerator, ReseedingSplittableRandomAdapter> INSTANCES = new WeakHashMap<>();
  @Nullable private static ReseedingSplittableRandomAdapter defaultInstance;
  protected final SeedGenerator seedGenerator;
  private transient RandomSeederThread seederThread; // Transient to work around Oracle bug 9050586
  private transient ThreadLocal<SingleThreadSplittableRandomAdapter> threadLocal;

  /**
   * Single instance per SeedGenerator.
   *
   * @param seedGenerator The seed generator this adapter will use.
   */
  private ReseedingSplittableRandomAdapter(SeedGenerator seedGenerator) throws SeedException {
    super(seedGenerator);
    this.seedGenerator = seedGenerator;
    initTransientFields();
  }

  public static synchronized ReseedingSplittableRandomAdapter getDefaultInstance()
      throws SeedException {
    if (defaultInstance == null) {
      defaultInstance = new ReseedingSplittableRandomAdapter(DefaultSeedGenerator.getInstance());
    }
    return defaultInstance;
  }

  @EnsuresNonNull("threadLocal")
  @Override
  protected void initTransientFields(
      @UnderInitialization(ReseedingSplittableRandomAdapter.class)ReseedingSplittableRandomAdapter this) {
    if (!superConstructorFinished) {
      return;
    }
    seederThread = RandomSeederThread.getInstance(seedGenerator);
    threadLocal = ThreadLocal.withInitial(() -> {
      try {
        return new SingleThreadSplittableRandomAdapter(seedGenerator);
      } catch (SeedException e) {
        throw new RuntimeException(e);
      }
    });
    super.initTransientFields();
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
