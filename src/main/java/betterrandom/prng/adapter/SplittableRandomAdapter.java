package betterrandom.prng.adapter;

import betterrandom.seed.SeedException;
import betterrandom.seed.SeedGenerator;
import java.util.SplittableRandom;

/**
 * Thread-safe version of {@link SingleThreadSplittableRandomAdapter}.
 */
public class SplittableRandomAdapter extends SingleThreadSplittableRandomAdapter {

  private static final long serialVersionUID = 2190439512972880590L;

  private transient ThreadLocal<SplittableRandom> threadLocal;

  public SplittableRandomAdapter(SeedGenerator seedGenerator) throws SeedException {
    super(seedGenerator);
  }

  @Override
  protected void initTransientFields() {
    super.initTransientFields();
    threadLocal = ThreadLocal.withInitial(underlying::split);
  }

  @Override
  protected SplittableRandom getSplittableRandom() {
    return threadLocal.get();
  }

  /**
   * {@inheritDoc} Applies only to the calling thread.
   */
  @Override
  public synchronized void setSeed(long seed) {
    threadLocal.set(new SplittableRandom(seed));
  }

  @Override
  public boolean equals(Object o) {
    return this == o
        || (o instanceof SplittableRandomAdapter
        && super.equals(o));
  }

  @Override
  public int hashCode() {
    return super.hashCode() + 1;
  }
}
