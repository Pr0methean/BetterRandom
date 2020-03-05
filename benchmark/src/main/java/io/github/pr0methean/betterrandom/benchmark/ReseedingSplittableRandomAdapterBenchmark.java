package io.github.pr0methean.betterrandom.benchmark;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.DEFAULT_INSTANCE;

import io.github.pr0methean.betterrandom.prng.adapter.SplittableRandomAdapter;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.RandomSeeder;
import java.util.Random;

public class ReseedingSplittableRandomAdapterBenchmark extends AbstractRandomBenchmarkWithReseeding {

  private final RandomSeeder thread = new RandomSeeder(DEFAULT_INSTANCE);

  @Override protected Random createPrng() throws SeedException {
    return new SplittableRandomAdapter(DEFAULT_INSTANCE, reseeding ? thread : null);
  }
}
