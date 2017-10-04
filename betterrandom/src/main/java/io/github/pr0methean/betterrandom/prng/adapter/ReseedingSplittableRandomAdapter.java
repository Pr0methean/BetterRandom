package io.github.pr0methean.betterrandom.prng.adapter;

import static org.checkerframework.checker.nullness.NullnessUtil.castNonNull;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A version of {@link SplittableRandomAdapter} that uses a {@link RandomSeederThread} to replace
 * each thread's {@link SplittableRandom} with a reseeded one as frequently as possible, but not
 * more frequently than it is being used.
 *
 * @author Chris Hennick
 */
public class ReseedingSplittableRandomAdapter extends BaseSplittableRandomAdapter {

  private static final long serialVersionUID = 6301096404034224037L;
  @SuppressWarnings("StaticCollection")
  private static final Map<SeedGenerator, ReseedingSplittableRandomAdapter> INSTANCES =
      Collections.synchronizedMap(new WeakHashMap<>(1));
  protected final SeedGenerator seedGenerator;
  @SuppressWarnings({"ThreadLocalNotStaticFinal",
      "InstanceVariableMayNotBeInitializedByReadObject"})
  private transient ThreadLocal<SingleThreadSplittableRandomAdapter> threadLocal;

  /**
   * Single instance per SeedGenerator.
   *
   * @param seedGenerator The seed generator this adapter will use.
   */
  private ReseedingSplittableRandomAdapter(final SeedGenerator seedGenerator) throws SeedException {
    super(seedGenerator.generateSeed(Long.BYTES));
    this.seedGenerator = seedGenerator;
    initSubclassTransientFields();
  }

  /**
   * @return The instance backed by the {@link DefaultSeedGenerator}.
   * @throws SeedException if the {@link DefaultSeedGenerator} throws one while generating the
   *     initial seed.
   */
  @SuppressWarnings("NonThreadSafeLazyInitialization")
  public static ReseedingSplittableRandomAdapter getDefaultInstance()
      throws SeedException {
    return getInstance(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  }

  /**
   * @param seedGenerator a {@link SeedGenerator} object.
   * @return the ReseedingSplittableRandomAdapter backed by {@code seedGenerator}.
   * @throws SeedException if {@code seedGenerator} throws one while generating the initial
   *     seed.
   */
  @SuppressWarnings("SynchronizationOnStaticField")
  public static ReseedingSplittableRandomAdapter getInstance(final SeedGenerator seedGenerator)
      throws SeedException {
    synchronized (INSTANCES) {
      try {
        return INSTANCES.computeIfAbsent(seedGenerator, seedGen -> {
          try {
            return new ReseedingSplittableRandomAdapter(seedGen);
          } catch (final SeedException e) {
            throw new RuntimeException(e);
          }
        });
      } catch (final RuntimeException e) {
        final Throwable cause = e.getCause();
        if (cause instanceof SeedException) {
          throw (SeedException) cause;
        } else {
          throw e;
        }
      }
    }
  }

  @Override
  protected ToStringHelper addSubclassFields(final ToStringHelper original) {
    return original.add("threadLocal", threadLocal);
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    initSubclassTransientFields();
  }

  private ReseedingSplittableRandomAdapter readResolve() throws IOException {
    try {
      return getInstance(seedGenerator);
    } catch (final SeedException e) {
      throw new IOException(e);
    }
  }

  @EnsuresNonNull({"threadLocal", "seederThread"})
  private void initSubclassTransientFields(
      @UnknownInitialization(BaseSplittableRandomAdapter.class)ReseedingSplittableRandomAdapter this) {
    if (threadLocal == null) {
      threadLocal = ThreadLocal.withInitial(() -> {
        try {
          return new SingleThreadSplittableRandomAdapter(castNonNull(seedGenerator));
        } catch (final SeedException e) {
          throw new RuntimeException(e);
        }
      });
    }
    seederThread.set(RandomSeederThread.getInstance(castNonNull(seedGenerator)));
  }

  @Override
  protected SplittableRandom getSplittableRandom() {
    final SingleThreadSplittableRandomAdapter adapterForThread = threadLocal.get();
    castNonNull(seederThread.get()).add(adapterForThread);
    return adapterForThread.getSplittableRandom();
  }

  @Override
  public boolean equals(final @Nullable Object o) {
    return (this == o)
        || ((o instanceof ReseedingSplittableRandomAdapter)
        && seedGenerator.equals(((ReseedingSplittableRandomAdapter) o).seedGenerator));
  }

  @Override
  protected void setSeedInternal(@UnknownInitialization ReseedingSplittableRandomAdapter this,
      final byte[] seed) {
    this.seed = seed.clone();
    if (entropyBits == null) {
      entropyBits = new AtomicLong(0);
    }
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
