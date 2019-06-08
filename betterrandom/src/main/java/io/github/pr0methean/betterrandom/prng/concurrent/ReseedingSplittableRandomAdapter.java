package io.github.pr0methean.betterrandom.prng.concurrent;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.WeakHashMap;
import javax.annotation.Nullable;

/**
 * Like {@link SplittableRandomAdapter}, but uses a {@link RandomSeederThread} to replace each
 * thread's {@link SplittableRandom} with a reseeded one as frequently as possible, but not more
 * frequently than it is being used.
 * @author Chris Hennick
 */
public class ReseedingSplittableRandomAdapter extends BaseSplittableRandomAdapter {

  private static final long serialVersionUID = 6301096404034224037L;
  @SuppressWarnings("StaticCollection")
  private static final Map<RandomSeederThread, ReseedingSplittableRandomAdapter> INSTANCES =
      Collections.synchronizedMap(new WeakHashMap<>(1));
  private final SeedGenerator seedGenerator;
  @SuppressWarnings(
      {"ThreadLocalNotStaticFinal", "InstanceVariableMayNotBeInitializedByReadObject"})
  private transient ThreadLocal<SingleThreadSplittableRandomAdapter> threadLocal;

  /**
   * Single instance per SeedGenerator.
   * @param seedGenerator The seed generator this adapter will use.
   * @param randomSeeder
   */
  private ReseedingSplittableRandomAdapter(final SeedGenerator seedGenerator, RandomSeederThread randomSeeder) throws SeedException {
    super(seedGenerator.generateSeed(Long.BYTES));
    this.seedGenerator = seedGenerator;
    this.randomSeeder.set(randomSeeder);
    initSubclassTransientFields();
  }

  @Deprecated
  public static ReseedingSplittableRandomAdapter getInstance(final SeedGenerator seedGenerator)
      throws SeedException {
    return getInstance(new RandomSeederThread(seedGenerator), seedGenerator);
  }

  /**
   * Returns the instance backed by the given {@link SeedGenerator}.
   * @param randomSeeder The random seeder the returned adapter is to use for reseeding.
   * @param seedGenerator The generator to use for initial seeding, if the instance doesn't already
   *     exist.
   * @return the ReseedingSplittableRandomAdapter backed by {@code randomSeeder}.
   * @throws SeedException if {@code randomSeeder} throws one while generating the initial
   *     seed.
   */
  @SuppressWarnings("SynchronizationOnStaticField")
  public static ReseedingSplittableRandomAdapter getInstance(final RandomSeederThread randomSeeder,
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

  @Override
  public void setRandomSeeder(final RandomSeederThread randomSeeder) {
    if (!this.randomSeeder.get().equals(randomSeeder)) {
      throw new UnsupportedOperationException(
          "ReseedingSplittableRandomAdapter's binding to RandomSeederThread is immutable");
    }
  }

  @Override public boolean usesParallelStreams() {
    return true;
  }

  @Override protected ToStringHelper addSubclassFields(final ToStringHelper original) {
    return original.add("threadLocal", threadLocal);
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    initSubclassTransientFields();
  }

  private ReseedingSplittableRandomAdapter readResolve() {
    return getInstance(seedGenerator);
  }

  private void initSubclassTransientFields() {
    if (threadLocal == null) {
      threadLocal =
          ThreadLocal.withInitial(() -> new SingleThreadSplittableRandomAdapter(seedGenerator));
    }
  }

  @Override protected SplittableRandom getSplittableRandom() {
    final SingleThreadSplittableRandomAdapter adapterForThread = threadLocal.get();
    adapterForThread.setRandomSeeder(new RandomSeederThread(seedGenerator));
    return adapterForThread.getSplittableRandom();
  }

  @Override protected void debitEntropy(final long bits) {
    // Necessary because our inherited next* methods read straight through to the SplittableRandom.
    threadLocal.get().debitEntropy(bits);
  }

  @Override public boolean equals(@Nullable final Object o) {
    return (this == o) || ((o instanceof ReseedingSplittableRandomAdapter) && seedGenerator
        .equals(((ReseedingSplittableRandomAdapter) o).seedGenerator));
  }

  @Override protected void setSeedInternal(final byte[] seed) {
    this.seed = seed.clone();
  }

  @Override public int hashCode() {
    return seedGenerator.hashCode() + 1;
  }

  @Override public String toString() {
    return "ReseedingSplittableRandomAdapter using " + seedGenerator;
  }
}
