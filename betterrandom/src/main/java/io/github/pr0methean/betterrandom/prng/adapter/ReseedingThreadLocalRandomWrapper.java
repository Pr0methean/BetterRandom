package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.seed.RandomSeeder;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.SerializableFunction;
import io.github.pr0methean.betterrandom.util.SerializableLongFunction;
import io.github.pr0methean.betterrandom.util.SerializableSupplier;
import java.util.Random;

/**
 * A {@link ThreadLocalRandomWrapper} that reseeds all its instances using a
 * {@link RandomSeeder}.
 */
public class ReseedingThreadLocalRandomWrapper<T extends BaseRandom> extends ThreadLocalRandomWrapper<T> {

  private static final long serialVersionUID = -3235519018032714059L;

  /**
   * Wraps the given {@link SerializableSupplier}. Uses the given {@link RandomSeeder} to reseed PRNGs,
   * but not to initialize them unless the {@link SerializableSupplier} does so. This PRNG
   * will be serializable as long as the {@link SerializableSupplier}'s captures are serializable.
   *
   * @param initializer a supplier that will be called to provide the initial {@link BaseRandom}
   *     for each thread.
   * @param seedGenerator The seed generation strategy whose {@link RandomSeeder} will be
   *     used to reseed each thread's PRNG.
   */
  public ReseedingThreadLocalRandomWrapper(final SeedGenerator seedGenerator,
      final SerializableSupplier<? extends T> initializer) {
    this(initializer, new RandomSeeder(seedGenerator));
  }

  /**
   * Wraps the given {@link SerializableSupplier}. Uses the given {@link RandomSeeder} to reseed PRNGs,
   * but not to initialize them unless the {@link SerializableSupplier} does so. This PRNG
   * will be serializable if the {@link SerializableSupplier}'s captures are serializable.
   *
   * @param initializer a supplier that will be called to provide the initial
   *     {@link BaseRandom} for each thread
   * @param randomSeederThread a random seeder that will reseed the PRNG for each thread when
   *     necessary
   */
  public ReseedingThreadLocalRandomWrapper(final SerializableSupplier<? extends T> initializer,
      final RandomSeeder randomSeederThread) {
    super(() -> {
      final T out = initializer.get();
      out.setRandomSeeder(randomSeederThread);
      return out;
    });
    randomSeeder.set(randomSeederThread);
  }

  /**
   * Wraps a seed generator and a function that takes a seed byte array as input. This
   * ReseedingThreadLocalRandomWrapper will be serializable if the {@link SerializableFunction}'s
   * captures are serializable.
   *
   * @param seedSize the size of seed arrays to generate.
   * @param seedGenerator The seed generation strategy that will provide the seed value for each
   *     thread's {@link BaseRandom}, both at initialization and through the
   *     corresponding {@link RandomSeeder}.
   * @param creator a {@link SerializableFunction} that creates a {@link BaseRandom} from each seed.
   *     Probably a constructor reference.
   *
   * @throws SeedException if {@code seedGenerator} fails to generate an initial seed
   */
  public ReseedingThreadLocalRandomWrapper(final int seedSize, final SeedGenerator seedGenerator,
      final SerializableFunction<byte[], ? extends T> creator) throws SeedException {
    this(seedSize, new RandomSeeder(seedGenerator), creator, seedGenerator);
  }

  /**
   * Wraps a seed generator and a function that takes a seed byte array as input. This
   * ReseedingThreadLocalRandomWrapper will be serializable if the {@link SerializableFunction}'s
   * captures are serializable.
   *
   * @param seedSize the size of seed arrays to generate.
   * @param randomSeederThread The random seeder to use for reseeding.
   * @param creator a {@link SerializableFunction} that creates a {@link BaseRandom} from each seed.
   * @param seedGenerator the seed generator for initialization.
   *
   * @throws SeedException if {@code seedGenerator} fails to generate an initial seed
   */
  private ReseedingThreadLocalRandomWrapper(final int seedSize,
      final RandomSeeder randomSeederThread, final SerializableFunction<byte[], ? extends T> creator,
      SeedGenerator seedGenerator)
      throws SeedException {
    super(seedSize, seedGenerator, seed -> {
          final T out = creator.apply(seed);
          out.setRandomSeeder(randomSeederThread);
          return out;
        });
    randomSeeder.set(randomSeederThread);
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
  public static ReseedingThreadLocalRandomWrapper<BaseRandom> wrapLegacy(
      final SerializableLongFunction<Random> legacyCreator, final SeedGenerator seedGenerator) {
    return new ReseedingThreadLocalRandomWrapper<>(Long.BYTES, seedGenerator,
        bytes -> new RandomWrapper<>(legacyCreator.apply(BinaryUtils.convertBytesToLong(bytes))));
  }

  @Override public void setRandomSeeder(final RandomSeeder randomSeeder) {
    if (this.randomSeeder.get() != randomSeeder) {
      throw new UnsupportedOperationException(
          "ReseedingThreadLocalRandomWrapper's binding to LegacyRandomSeeder is immutable");
    }
  }

  @Override public RandomSeeder getRandomSeeder() {
    return randomSeeder.get();
  }
}
