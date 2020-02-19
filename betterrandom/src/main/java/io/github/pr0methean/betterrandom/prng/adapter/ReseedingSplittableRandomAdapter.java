package io.github.pr0methean.betterrandom.prng.adapter;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeeder;
import io.github.pr0methean.betterrandom.util.Java8Constants;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java8.util.SplittableRandom;
import javax.annotation.Nullable;

/**
 * Thread-safe PRNG that wraps a {@link ThreadLocal}&lt;{@link SplittableRandom}&gt;. Registers each
 * thread's instance with a {@link SimpleRandomSeeder} to replace its {@link SplittableRandom} with
 * a reseeded one as frequently as possible, but not more frequently than it is being used.
 *
 * @author Chris Hennick
 */
public class ReseedingSplittableRandomAdapter extends BaseSplittableRandomAdapter {

  private static final long serialVersionUID = 6301096404034224037L;
  @Deprecated

  private static final Map<SimpleRandomSeeder, ReseedingSplittableRandomAdapter> INSTANCES = Collections
      .synchronizedMap(new WeakHashMap<SimpleRandomSeeder, ReseedingSplittableRandomAdapter>(1));
  private final SeedGenerator seedGenerator;
  /**
   * A thread-local delegate.
   */
  @SuppressWarnings({"ThreadLocalNotStaticFinal", "TransientFieldNotInitialized"})
  protected transient ThreadLocal<? extends BaseRandom> threadLocal;

  /**
   * Creates an instance.
   *
   * @param seedGenerator the seed generator that will generate an initial seed for each thread
   * @param randomSeeder the {@link SimpleRandomSeeder} that will generate a seed for a new
   *     {@link SplittableRandom} instance whenever each thread's instance needs reseeding
   * @throws SeedException if {@code seedGenerator} fails to generate an initial seed
   */
  public ReseedingSplittableRandomAdapter(final SeedGenerator seedGenerator,
      @Nullable SimpleRandomSeeder randomSeeder) throws SeedException {
    super(new byte[Java8Constants.LONG_BYTES]);
    this.seedGenerator = seedGenerator;
    this.randomSeeder.set(randomSeeder);
    threadLocal = new ThreadLocal<BaseRandom>() {
      @Override protected BaseRandom initialValue() {
        return createDelegate();
      }
    };
  }

  /**
   * Creates an instance that uses the same {@link SeedGenerator} for reseeding and for initial
   * seeding, and whose {@link SimpleRandomSeeder} uses a
   * {@link SimpleRandomSeeder.DefaultThreadFactory}.
   *
   * @param seedGenerator the seed generator that will generate an initial seed for each thread
   * @throws SeedException if {@code seedGenerator} fails to generate an initial seed
   */
  public ReseedingSplittableRandomAdapter(final SeedGenerator seedGenerator) {
    this(seedGenerator, new SimpleRandomSeeder(seedGenerator));
  }

  /**
   * Creates the delegate for the calling thread.
   *
   * @return the thread-local delegate
   */
  protected BaseRandom createDelegate() {
      SingleThreadSplittableRandomAdapter threadAdapter =
          new SingleThreadSplittableRandomAdapter(this.seedGenerator);
      threadAdapter.setRandomSeeder(this.randomSeeder.get());
      return threadAdapter;
  }

  /**
   * Returns an instance backed by the given {@link SimpleRandomSeeder}. Will return the same
   * instance if called more than once for the same {@link SimpleRandomSeeder}.
   *
   * @param randomSeeder The random seeder the returned adapter is to use for reseeding.
   * @param seedGenerator The generator to use for initial seeding, if the instance doesn't already
   *     exist.
   * @return the ReseedingSplittableRandomAdapter backed by {@code randomSeeder}.
   * @throws SeedException if {@code randomSeeder} throws one while generating the initial
   *     seed.
   *
   * @deprecated Callers should instead construct their own instances.
   */
  @Deprecated
  @SuppressWarnings("SynchronizationOnStaticField")
  public static ReseedingSplittableRandomAdapter getInstance(
      @Nullable final SimpleRandomSeeder randomSeeder,
      final SeedGenerator seedGenerator) throws SeedException {
    ReseedingSplittableRandomAdapter instance = INSTANCES.get(randomSeeder);
    if (instance == null) {
      synchronized (INSTANCES) {
        instance = INSTANCES.get(randomSeeder);
        if (instance == null) {
          instance = new ReseedingSplittableRandomAdapter(seedGenerator, randomSeeder);
          INSTANCES.put(randomSeeder, instance);
        }
      }
    }
    return instance;
  }

  @Override public long getEntropyBits() {
    return threadLocal.get().getEntropyBits();
  }

  @Override public byte[] getSeed() {
    return threadLocal.get().getSeed();
  }

  @Override public void setRandomSeeder(@Nullable final SimpleRandomSeeder randomSeeder) {
    if (!Objects.equals(this.randomSeeder.get(), randomSeeder)) {
      throw new UnsupportedOperationException(
          "ReseedingSplittableRandomAdapter's binding to SimpleRandomSeeder is immutable");
    }
  }

  @Override public boolean usesParallelStreams() {
    return true;
  }

  @Override protected ToStringHelper addSubclassFields(final ToStringHelper original) {
    return original.add("randomSeeder", randomSeeder.get()).add("seedGenerator", seedGenerator);
  }

  @Override protected SplittableRandom getSplittableRandom() {
    final SingleThreadSplittableRandomAdapter adapterForThread
        = (SingleThreadSplittableRandomAdapter) threadLocal.get();
    return adapterForThread.getSplittableRandom();
  }

  @Override public double nextGaussian() {
    return threadLocal.get().nextGaussian();
  }

  @Override protected void debitEntropy(final long bits) {
    // Necessary because our inherited next* methods read straight through to the SplittableRandom.
    ((SingleThreadSplittableRandomAdapter) threadLocal.get()).debitEntropy(bits);
  }

  @Override protected void setSeedInternal(final byte[] seed) {
    this.seed = seed.clone();
  }

  @Override public String toString() {
    return "ReseedingSplittableRandomAdapter using " + randomSeeder;
  }

  @Override public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReseedingSplittableRandomAdapter that = (ReseedingSplittableRandomAdapter) o;
    return Objects.equals(randomSeeder.get(), that.randomSeeder.get())
        && Objects.equals(seedGenerator, that.seedGenerator);
  }

  @Override public int hashCode() {
    return Objects.hash(randomSeeder.get(), seedGenerator);
  }
}
