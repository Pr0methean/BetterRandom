package io.github.pr0methean.betterrandom.prng.adapter;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.seed.RandomSeeder;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.MoreCollections;
import io.github.pr0methean.betterrandom.util.SerializableFunction;
import io.github.pr0methean.betterrandom.util.SerializableLongFunction;
import io.github.pr0methean.betterrandom.util.SerializableSupplier;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * Wraps a {@link ThreadLocal}&lt;{@link BaseRandom}&gt; in order to provide concurrency that most
 * implementations of {@link BaseRandom} can't implement naturally.
 */
public class ThreadLocalRandomWrapper<T extends BaseRandom> extends RandomWrapper<T> {

  private static final long serialVersionUID = 1199235201518562359L;
  private final SerializableSupplier<? extends T> initializer;
  private final SerializableFunction<byte[], ? extends T> initializerForSeed;
  private final int seedSize;
  private transient Set<Thread> threadsInitializedFor;
  /**
   * Holds the delegate for each thread.
   */
  @SuppressWarnings({"ThreadLocalNotStaticFinal"}) protected transient ThreadLocal<T> threadLocal;

  @Override protected void initTransientFields() {
    super.initTransientFields();
    threadsInitializedFor = MoreCollections.createSynchronizedWeakHashSet();
  }

  private ThreadLocalRandomWrapper(int seedSize, SerializableSupplier<? extends T> undecoratedInitializer,
      SerializableFunction<byte[], ? extends T> initializerForSeed) {
    super(null);
    this.seedSize = seedSize;
    initializer = () -> {
      threadsInitializedFor.add(Thread.currentThread());
      return undecoratedInitializer.get();
    };
    this.initializerForSeed = initializerForSeed;
    threadLocal = ThreadLocal.withInitial(initializer);
  }

  /**
   * Wraps the given {@link Supplier}. This ThreadLocalRandomWrapper will be serializable if the
   * {@link Supplier} is serializable.
   *
   * @param initializer a supplier that will be called to provide the initial {@link BaseRandom}
   *     for each thread.
   */
  public ThreadLocalRandomWrapper(final SerializableSupplier<? extends T> initializer) {
    this(initializer.get().getNewSeedLength(), initializer, seed -> {
      T out = initializer.get();
      out.setSeed(seed);
      return out;
    });
  }

  /**
   * Wraps a seed generator and a function that takes a seed byte array as input. This
   * ThreadLocalRandomWrapper will be serializable if the {@link Function} is serializable.
   *
   * @param seedSize the size of seed arrays to generate.
   * @param seedGenerator The seed generation strategy that will provide the seed value for each
   *     thread's {@link BaseRandom}.
   * @param creator a {@link Function} that creates a {@link BaseRandom} from each seed.
   *     Probably a constructor reference.
   */
  public ThreadLocalRandomWrapper(final int seedSize, final SeedGenerator seedGenerator,
      final SerializableFunction<byte[], ? extends T> creator) {
    this(seedSize, () -> creator.apply(seedGenerator.generateSeed(seedSize)), creator);
  }

  /**
   * Uses this class and {@link RandomWrapper} to decorate any implementation of {@link Random} that
   * can be constructed from a {@code long} seed into a fully-concurrent one.
   *
   * @param legacyCreator a function that provides the {@link Random} that underlies the
   *     returned wrapper on each thread, taking a seed as input.
   * @param seedGenerator the seed generator whose output will be fed to {@code legacyCreator}.
   * @return a ThreadLocalRandomWrapper decorating instances created by {@code legacyCreator}.
   */
  public static ThreadLocalRandomWrapper<BaseRandom> wrapLegacy(
      final SerializableLongFunction<Random> legacyCreator,
      final SeedGenerator seedGenerator) {
    return new ThreadLocalRandomWrapper<>(Long.BYTES, seedGenerator,
        bytes -> new RandomWrapper<>(legacyCreator.apply(BinaryUtils.convertBytesToLong(bytes))));
  }

  @Nullable @Override public RandomSeeder getRandomSeeder() {
    return null;
  }

  /**
   * Not supported, because this class uses a thread-local seed.
   *
   * @param randomSeeder ignored.
   * @throws UnsupportedOperationException always.
   */
  @Override public void setRandomSeeder(@Nullable final RandomSeeder randomSeeder) {
    if (randomSeeder != null) {
      throw new UnsupportedOperationException("This can't be reseeded by a RandomSeeder");
    }
  }

  @Override protected boolean withProbabilityInternal(double probability) {
    throw new AssertionError("Caller should be delegating at a higher level");
  }

  @Override public boolean withProbability(final double probability) {
    return getWrapped().withProbability(probability);
  }

  @Override public long nextLong() {
    return getWrapped().nextLong();
  }

  @Override public long nextLong(final long bound) {
    return getWrapped().nextLong(bound);
  }

  @Override public int nextInt(final int origin, final int bound) {
    return getWrapped().nextInt(origin, bound);
  }

  @Override public long nextLong(final long origin, final long bound) {
    return getWrapped().nextLong(origin, bound);
  }

  @Override public T getWrapped() {
    return threadLocal.get();
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    threadLocal = ThreadLocal.withInitial(initializer);
  }

  @Override public void nextBytes(final byte[] bytes) {
    getWrapped().nextBytes(bytes);
  }

  @Override public int nextInt() {
    return getWrapped().nextInt();
  }

  @Override public int nextInt(final int bound) {
    return getWrapped().nextInt(bound);
  }

  @Override protected long nextLongNoEntropyDebit() {
    throw new AssertionError("Caller should be delegating at a higher level");
  }

  @Override public boolean nextBoolean() {
    return getWrapped().nextBoolean();
  }

  @Override public float nextFloat() {
    return getWrapped().nextFloat();
  }

  @Override public double nextDoubleNoEntropyDebit() {
    throw new AssertionError("Caller should be delegating at a higher level");
  }

  @Override public double nextGaussian() {
    return getWrapped().nextGaussian();
  }

  @Override public double nextDouble() {
    return getWrapped().nextDouble();
  }

  @Override protected ToStringHelper addSubclassFields(final ToStringHelper original) {
    return original.add("wrapped on this thread", getWrapped().dump());
  }

  @Override public boolean preferSeedWithLong() {
    final int newSeedLength = getNewSeedLength();
    return (newSeedLength > 0) && (newSeedLength <= Long.BYTES);
  }

  @Override public byte[] getSeed() {
    return getWrapped().getSeed();
  }

  @SuppressWarnings("VariableNotUsedInsideIf") @Override public void setSeed(final long seed) {
    if (threadLocal != null) {
      getWrapped().setSeed(seed);
    }
  }

  @Override
  protected void setSeedInternal(final byte[] seed) {
    if (seed == null) {
      throw new IllegalArgumentException("Seed must not be null");
    }
    if (threadLocal != null) {
      if (isInitializedForCurrentThread()) {
        getWrapped().setSeed(seed);
      } else {
        threadLocal.set(initializerForSeed.apply(seed));
        threadsInitializedFor.add(Thread.currentThread());
      }
    }
    if (this.seed == null) {
      this.seed = seed.clone(); // Needed for serialization
    }
  }

  @Override protected void debitEntropy(final long bits) {
    throw new AssertionError("Caller should be delegating at a higher level");
  }

  @Override public long getEntropyBits() {
    return isInitializedForCurrentThread() ? getWrapped().getEntropyBits() : seedSize * 8L;
  }

  @Override public int getNewSeedLength() {
    return isInitializedForCurrentThread() ? getWrapped().getNewSeedLength() : seedSize;
  }

  private boolean isInitializedForCurrentThread() {
    return threadLocal != null && threadsInitializedFor.contains(Thread.currentThread());
  }
}
