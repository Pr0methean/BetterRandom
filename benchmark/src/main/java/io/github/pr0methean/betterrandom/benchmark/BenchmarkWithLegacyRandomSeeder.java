package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.seed.LegacyRandomSeeder;
import io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator;
import java.util.Random;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;

/**
 * An {@link AbstractRandomBenchmarkWithReseeding} that uses a {@link LegacyRandomSeeder} rather
 * than a {@link io.github.pr0methean.betterrandom.seed.RandomSeeder}.
 *
 * @param <T> the PRNG class
 */
public abstract class BenchmarkWithLegacyRandomSeeder<T extends Random>
    extends AbstractRandomBenchmarkWithReseeding<T> {
  private static final LegacyRandomSeeder RANDOM_SEEDER = new LegacyRandomSeeder(
      SecureRandomSeedGenerator.DEFAULT_INSTANCE);

  @Override
  @TearDown(Level.Trial)
  public void tearDown() {
    if (reseeding) {
      RANDOM_SEEDER.removeLegacyRandoms(prng);
    }
  }

  @Override
  @Setup(Level.Trial)
  public void setUp() throws Exception {
    prng = createPrng();
    if (reseeding) {
      RANDOM_SEEDER.addLegacyRandoms(prng);
    }
  }
}
