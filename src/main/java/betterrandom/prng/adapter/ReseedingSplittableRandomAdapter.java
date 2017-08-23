package betterrandom.prng.adapter;

import betterrandom.seed.DefaultSeedGenerator;
import betterrandom.seed.RandomSeederThread;
import betterrandom.seed.SeedException;
import betterrandom.seed.SeedGenerator;
import java.util.SplittableRandom;
import java.util.WeakHashMap;

/**
 * A version of {@link SplittableRandomAdapter} that uses a {@link RandomSeederThread} to replace
 * each thread's {@link SplittableRandom} with a reseeded one as frequently as possible, but not
 * more frequently than it is being used.
 */
public class ReseedingSplittableRandomAdapter extends SplittableRandomAdapter {

  private static final long serialVersionUID = 6301096404034224037L;
  private static final WeakHashMap<SeedGenerator, ReseedingSplittableRandomAdapter> INSTANCES = new WeakHashMap<>();
  private static ReseedingSplittableRandomAdapter defaultInstance;
  protected final SeedGenerator seedGenerator;
  private final RandomSeederThread seederThread;
  private transient ThreadLocal<SingleThreadSplittableRandomAdapter> threadLocal;

  /**
   * Single instance per SeedGenerator.
   *
   * @param seedGenerator The seed generator this adapter will use.
   */
  private ReseedingSplittableRandomAdapter(SeedGenerator seedGenerator) throws SeedException {
    super(seedGenerator);
    this.seedGenerator = seedGenerator;
    seederThread = RandomSeederThread.getInstance(seedGenerator);
  }

  public static synchronized ReseedingSplittableRandomAdapter getDefaultInstance()
      throws SeedException {
    if (defaultInstance == null) {
      defaultInstance = new ReseedingSplittableRandomAdapter(DefaultSeedGenerator.getInstance());
    }
    return defaultInstance;
  }

  @Override
  protected void initTransientFields() {
    super.initTransientFields();
    threadLocal = ThreadLocal.withInitial(() -> {
      try {
        return new SingleThreadSplittableRandomAdapter(seedGenerator);
      } catch (SeedException e) {
        throw new RuntimeException(e);
      }
    });
  }

  public ReseedingSplittableRandomAdapter getInstance(SeedGenerator seedGenerator)
      throws SeedException {
    try {
      return INSTANCES.computeIfAbsent(seedGenerator, seedGenerator_ -> {
        try {
          return new ReseedingSplittableRandomAdapter(seedGenerator_);
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
