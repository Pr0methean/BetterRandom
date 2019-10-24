package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeeder;
import io.github.pr0methean.betterrandom.util.Java8Constants;
import io.github.pr0methean.betterrandom.util.SerializableFunction;
import io.github.pr0methean.betterrandom.util.SerializableSupplier;
import java.util.Random;
import java8.util.function.Function;
import java8.util.function.LongFunction;
import java8.util.function.Supplier;

/**
 * A {@link ThreadLocalRandomWrapper} that reseeds all its instances using a
 * {@link SimpleRandomSeeder}.
 */
public class ReseedingThreadLocalRandomWrapper extends ThreadLocalRandomWrapper {

  private static final long serialVersionUID = -3235519018032714059L;

  /**
   * Wraps the given {@link Supplier}. Uses the given {@link SimpleRandomSeeder} to reseed PRNGs,
   * but not to initialize them unless the {@link Supplier} does so. This ThreadLocalRandomWrapper
   * will be serializable if the {@link Supplier} is serializable.
   *
   * @param initializer a supplier that will be called to provide the initial {@link BaseRandom}
   *     for each thread.
   * @param seedGenerator The seed generation strategy whose {@link SimpleRandomSeeder} will be
   *     used to reseed each thread's PRNG.
   */
  public ReseedingThreadLocalRandomWrapper(final SeedGenerator seedGenerator,
      final Supplier<? extends BaseRandom> initializer) {
    this(initializer, new SimpleRandomSeeder(seedGenerator));
  }

  /**
   * Wraps the given {@link Supplier}. Uses the given {@link SimpleRandomSeeder} to reseed PRNGs,
   * but not to initialize them unless the {@link Supplier} does so. This ThreadLocalRandomWrapper
   * will be serializable if the {@link Supplier} is serializable.
   *
   * @param initializer a supplier that will be called to provide the initial
   *     {@link BaseRandom} for each thread
   * @param randomSeederThread a random seeder that will reseed the PRNG for each thread when
   *     necessary
   */
  public ReseedingThreadLocalRandomWrapper(final Supplier<? extends BaseRandom> initializer,
      final SimpleRandomSeeder randomSeederThread) {
    super(new SerializableSupplier<BaseRandom>() {
      @Override public BaseRandom get() {
        final BaseRandom out = initializer.get();
        out.setRandomSeeder(randomSeederThread);
        return out;
      }
    });
    randomSeeder.set(randomSeederThread);
  }

  /**
   * Wraps a seed generator and a function that takes a seed byte array as input. This
   * ReseedingThreadLocalRandomWrapper will be serializable if the {@link Function} is
   * serializable.
   *
   * @param seedSize the size of seed arrays to generate.
   * @param seedGenerator The seed generation strategy that will provide the seed value for each
   *     thread's {@link BaseRandom}, both at initialization and through the
   *     corresponding {@link SimpleRandomSeeder}.
   * @param creator a {@link Function} that creates a {@link BaseRandom} from each seed.
   *     Probably a constructor reference.
   *
   * @throws SeedException if {@code seedGenerator} fails to generate an initial seed
   */
  public ReseedingThreadLocalRandomWrapper(final int seedSize, final SeedGenerator seedGenerator,
      final Function<byte[], ? extends BaseRandom> creator) throws SeedException {
    this(seedSize, new SimpleRandomSeeder(seedGenerator), creator, seedGenerator);
  }

  /**
   * Wraps a seed generator and a function that takes a seed byte array as input. This
   * ReseedingThreadLocalRandomWrapper will be serializable if the {@link Function} is
   * serializable.
   *
   * @param seedSize the size of seed arrays to generate.
   * @param randomSeederThread The random seeder to use for reseeding.
   * @param creator a {@link Function} that creates a {@link BaseRandom} from each seed.
   * @param seedGenerator the seed generator for initialization.
   *
   * @throws SeedException if {@code seedGenerator} fails to generate an initial seed
   */
  public ReseedingThreadLocalRandomWrapper(final int seedSize,
      final SimpleRandomSeeder randomSeederThread,
      final Function<byte[], ? extends BaseRandom> creator, SeedGenerator seedGenerator)
      throws SeedException {
    super(seedSize, seedGenerator, new SerializableFunction<byte[], BaseRandom>() {
      @Override public BaseRandom apply(byte[] seed) {
        final BaseRandom out = creator.apply(seed);
        out.setRandomSeeder(randomSeederThread);
        return out;
      }
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
  public static ReseedingThreadLocalRandomWrapper wrapLegacy(
      final LongFunction<Random> legacyCreator, final SeedGenerator seedGenerator) {
    return new ReseedingThreadLocalRandomWrapper(Java8Constants.LONG_BYTES, seedGenerator,
        wrapLongCreatorAsByteArrayCreator(legacyCreator));
  }

  @Override public void setRandomSeeder(final SimpleRandomSeeder randomSeeder) {
    if (this.randomSeeder.get() != randomSeeder) {
      throw new UnsupportedOperationException(
          "ReseedingThreadLocalRandomWrapper's binding to LegacyRandomSeeder is immutable");
    }
  }

  @Override public SimpleRandomSeeder getRandomSeeder() {
    return randomSeeder.get();
  }
}
