package io.github.pr0methean.betterrandom.prng.adapter;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.seed.RandomSeeder;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Random;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * Wraps a {@link ThreadLocal}&lt;{@link BaseRandom}&gt; in order to provide concurrency that most
 * implementations of {@link BaseRandom} can't implement naturally.
 */
public class ThreadLocalRandomWrapper extends RandomWrapper {

  private static final long serialVersionUID = 1199235201518562359L;
  private final Supplier<? extends BaseRandom>
      initializer;
  @Nullable private final Integer explicitSeedSize;
  /**
   * Holds the delegate for each thread.
   */
  @SuppressWarnings({"ThreadLocalNotStaticFinal"}) protected transient ThreadLocal<BaseRandom> threadLocal;

  /**
   * Wraps the given {@link Supplier}. This ThreadLocalRandomWrapper will be serializable if the
   * {@link Supplier} is serializable.
   *
   * @param initializer a supplier that will be called to provide the initial {@link BaseRandom}
   *     for each thread.
   */
  public ThreadLocalRandomWrapper(final Supplier<? extends BaseRandom> initializer) {
    super(0);
    this.initializer = initializer;
    threadLocal = ThreadLocal.withInitial(initializer);
    explicitSeedSize = null;
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
      final Function<byte[], ? extends BaseRandom> creator) {
    super(0);
    explicitSeedSize = seedSize;
    initializer = (Serializable & Supplier<BaseRandom>) (() -> creator
        .apply(seedGenerator.generateSeed(seedSize)));
    threadLocal = ThreadLocal.withInitial(initializer);
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
  public static ThreadLocalRandomWrapper wrapLegacy(final LongFunction<Random> legacyCreator,
      final SeedGenerator seedGenerator) {
    return new ThreadLocalRandomWrapper(Long.BYTES, seedGenerator,
        bytes -> new RandomWrapper(legacyCreator.apply(BinaryUtils.convertBytesToLong(bytes))));
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
      throw new UnsupportedOperationException("This can't be reseeded by a LegacyRandomSeeder");
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

  @Override public BaseRandom getWrapped() {
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
      final BaseRandom wrapped = getWrapped();
      wrapped.setSeed(seed);
    }
  }

  @SuppressWarnings("VariableNotUsedInsideIf") @Override
  protected void setSeedInternal(final byte[] seed) {
    if (seed == null) {
      throw new IllegalArgumentException("Seed must not be null");
    }
    if (threadLocal != null) {
      final BaseRandom wrapped = getWrapped();
      wrapped.setSeed(seed);
    }
    if (this.seed == null) {
      this.seed = seed.clone(); // Needed for serialization
    }
  }

  @Override protected void debitEntropy(final long bits) {
    throw new AssertionError("Caller should be delegating at a higher level");
  }

  @Override public long getEntropyBits() {
    return getWrapped().getEntropyBits();
  }

  @SuppressWarnings("VariableNotUsedInsideIf") @Override public int getNewSeedLength() {
    return (threadLocal == null) ? 0 :
        ((explicitSeedSize == null) ? getWrapped().getNewSeedLength() : explicitSeedSize);
  }
}
