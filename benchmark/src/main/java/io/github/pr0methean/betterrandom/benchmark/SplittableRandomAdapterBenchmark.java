package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.adapter.SplittableRandomAdapter;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.util.Random;

public class SplittableRandomAdapterBenchmark extends AbstractRandomBenchmark {

  @EntryPoint
  public SplittableRandomAdapterBenchmark() {
  }

  @Override
  protected Random createPrng() throws SeedException {
    return new SplittableRandomAdapter(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  }
}
