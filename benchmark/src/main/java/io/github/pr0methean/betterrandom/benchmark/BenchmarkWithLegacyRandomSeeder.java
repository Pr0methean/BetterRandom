package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.seed.LegacyRandomSeeder;
import io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;

public abstract class BenchmarkWithLegacyRandomSeeder extends AbstractRandomBenchmarkWithReseeding {
  private static final LegacyRandomSeeder RANDOM_SEEDER = new LegacyRandomSeeder(
      SecureRandomSeedGenerator.DEFAULT_INSTANCE);

  @Override
  @TearDown(Level.Trial)
  public void tearDown() {
    if (reseeding) {
      RANDOM_SEEDER.remove(prng);
    }
    super.tearDown();
  }

  @Override
  @Setup(Level.Trial)
  public void setUp() throws Exception {
    prng = createPrng();
    if (reseeding) {
      RANDOM_SEEDER.add(prng);
    }
  }

  @Override protected abstract Random createPrng() throws SeedException;
}
