package io.github.pr0methean.betterrandom.benchmark;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.DEFAULT_INSTANCE;

import io.github.pr0methean.betterrandom.prng.adapter.SplittableRandomAdapter;
import io.github.pr0methean.betterrandom.seed.RandomSeeder;
import io.github.pr0methean.betterrandom.seed.SeedException;

/**
 * Benchmark for {@link SplittableRandomAdapter}.
 */
public class SplittableRandomAdapterBenchmark extends AbstractRandomBenchmarkWithReseeding<SplittableRandomAdapter> {

  private final RandomSeeder thread = new RandomSeeder(DEFAULT_INSTANCE);

  @Override protected SplittableRandomAdapter createPrng() throws SeedException {
    return new SplittableRandomAdapter(DEFAULT_INSTANCE, reseeding ? thread : null);
  }
}
