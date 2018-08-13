package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import java.io.Serializable;
import java.util.Random;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * A {@link ThreadLocalRandomWrapper} that reseeds all its instances using the
 * {@link RandomSeederThread} for its {@link SeedGenerator}.
 */
public class ReseedingThreadLocalRandomWrapper extends ThreadLocalRandomWrapper {

  private static final long serialVersionUID = -3235519018032714059L;
  private final SeedGenerator seedGenerator;

  /**
   * Wraps the given {@link Supplier}. Uses the given {@link RandomSeederThread} to reseed PRNGs,
   * but not to initialize them unless the {@link Supplier} does so. This ThreadLocalRandomWrapper
   * will be serializable if the {@link Supplier} is serializable.
   * @param initializer a supplier that will be called to provide the initial {@link BaseRandom}
   *     for each thread.
   * @param seedGenerator The seed generation strategy whose {@link RandomSeederThread} will be
   *     used to reseed each thread's PRNG.
   */
  public ReseedingThreadLocalRandomWrapper(final SeedGenerator seedGenerator,
      final Supplier<? extends BaseRandom> initializer) throws SeedException {
    super((Serializable & Supplier<? extends BaseRandom>) () -> {
      final BaseRandom out = initializer.get();
      out.setSeedGenerator(seedGenerator);
      return out;
    });
    this.seedGenerator = seedGenerator;
  }

  /**
   * Wraps a seed generator and a function that takes a seed byte array as input. This
   * ReseedingThreadLocalRandomWrapper will be serializable if the {@link Function} is
   * serializable.
   * @param seedSize the size of seed arrays to generate.
   * @param seedGenerator The seed generation strategy that will provide the seed value for each
   *     thread's {@link BaseRandom}, both at initialization and through the corresponding {@link
   *     RandomSeederThread}.
   * @param creator a {@link Function} that creates a {@link BaseRandom} from each seed.
   *     Probably a constructor reference.
   */
  public ReseedingThreadLocalRandomWrapper(final int seedSize, final SeedGenerator seedGenerator,
      final Function<byte[], ? extends BaseRandom> creator) throws SeedException {
    super(seedSize, seedGenerator,
        (Serializable & Function<byte[], ? extends BaseRandom>) (seed) -> {
          final BaseRandom out = creator.apply(seed);
          out.setSeedGenerator(seedGenerator);
          return out;
        });
    this.seedGenerator = seedGenerator;
  }

  /**
   * Uses this class and {@link RandomWrapper} to decorate any implementation of {@link Random} that
   * can be constructed from a {@code long} seed into a fully-concurrent one.
   * @param legacyCreator a function that provides the {@link Random} that underlies the
   *     returned wrapper on each thread, taking a seed as input.
   * @param seedGenerator the seed generator whose output will be fed to {@code legacyCreator}.
   * @return a ThreadLocalRandomWrapper decorating instances created by {@code legacyCreator}.
   */
  public static ReseedingThreadLocalRandomWrapper wrapLegacy(
      final LongFunction<Random> legacyCreator, final SeedGenerator seedGenerator) {
    return new ReseedingThreadLocalRandomWrapper(Long.BYTES, seedGenerator,
        bytes -> new RandomWrapper(legacyCreator.apply(BinaryUtils.convertBytesToLong(bytes))));
  }

  @Override public SeedGenerator getSeedGenerator() {
    return seedGenerator;
  }
}
