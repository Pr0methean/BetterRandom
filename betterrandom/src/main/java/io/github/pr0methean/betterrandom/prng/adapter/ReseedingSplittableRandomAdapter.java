package io.github.pr0methean.betterrandom.prng.adapter;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeeder;
import java.util.Collections;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.WeakHashMap;

/**
 * Like {@link SplittableRandomAdapter}, but uses a {@link SimpleRandomSeeder} to replace each
 * thread's {@link SplittableRandom} with a reseeded one as frequently as possible, but not more
 * frequently than it is being used.
 *
 * @author Chris Hennick
 */
public class ReseedingSplittableRandomAdapter extends BaseSplittableRandomAdapter {

  private static final long serialVersionUID = 6301096404034224037L;
  private static final Map<SimpleRandomSeeder,
      ReseedingSplittableRandomAdapter>
      INSTANCES = Collections.synchronizedMap(new WeakHashMap<>(1));
  private final SeedGenerator seedGenerator;
  /**
   * A thread-local delegate.
   */
  @SuppressWarnings({"ThreadLocalNotStaticFinal", "TransientFieldNotInitialized"})
  protected transient ThreadLocal<BaseRandom> threadLocal;

  /**
   * Single instance per {@link SimpleRandomSeeder} except via subclasses.
   *
   * @param seedGenerator the seed generator that will generate an initial seed for each thread
   * @param randomSeeder the {@link SimpleRandomSeeder} that will generate a seed for a new
   *     {@link SplittableRandom} instance whenever each thread's instance needs reseeding
   * @throws SeedException if {@code seedGenerator} fails to generate an initial seed
   */
  protected ReseedingSplittableRandomAdapter(final SeedGenerator seedGenerator,
      SimpleRandomSeeder randomSeeder) throws SeedException {
    super(seedGenerator.generateSeed(Long.BYTES));
    this.seedGenerator = seedGenerator;
    this.randomSeeder.set(randomSeeder);
    threadLocal = ThreadLocal.withInitial(this::createDelegate);
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
   * Returns the instance backed by the given {@link SeedGenerator}.
   *
   * @param randomSeeder The random seeder the returned adapter is to use for reseeding.
   * @param seedGenerator The generator to use for initial seeding, if the instance doesn't already
   *     exist.
   * @return the ReseedingSplittableRandomAdapter backed by {@code randomSeeder}.
   * @throws SeedException if {@code randomSeeder} throws one while generating the initial
   *     seed.
   */
  @SuppressWarnings("SynchronizationOnStaticField")
  public static ReseedingSplittableRandomAdapter getInstance(final SimpleRandomSeeder randomSeeder,
      final SeedGenerator seedGenerator) throws SeedException {
    synchronized (INSTANCES) {
      return INSTANCES.computeIfAbsent(randomSeeder,
          randomSeeder_ -> new ReseedingSplittableRandomAdapter(seedGenerator, randomSeeder_));
    }
  }

  @Override public long getEntropyBits() {
    return threadLocal.get().getEntropyBits();
  }

  @Override public byte[] getSeed() {
    return threadLocal.get().getSeed();
  }

  @Override public void setRandomSeeder(final SimpleRandomSeeder randomSeeder) {
    if (!this.randomSeeder.get().equals(randomSeeder)) {
      throw new UnsupportedOperationException(
          "ReseedingSplittableRandomAdapter's binding to LegacyRandomSeeder is immutable");
    }
  }

  @Override public boolean usesParallelStreams() {
    return true;
  }

  @Override protected ToStringHelper addSubclassFields(final ToStringHelper original) {
    return original.add("randomSeeder", randomSeeder.get()).add("seedGenerator", seedGenerator);
  }

  /**
   * Returns the identical instance managed by this class, so that duplicates are eliminated.
   * Subclasses must override this method if the delegate held in {@link #threadLocal} is not always
   * a {@link SingleThreadSplittableRandomAdapter}.
   *
   * @return the instance returned by {@link #getInstance(SimpleRandomSeeder, SeedGenerator)}
   *     with this instance's parameters
   */
  protected Object readResolve() {
    return getInstance(randomSeeder.get(), seedGenerator);
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
    return randomSeeder.get().equals(that.randomSeeder.get());
  }

  @Override public int hashCode() {
    return randomSeeder.hashCode() + 1;
  }
}
