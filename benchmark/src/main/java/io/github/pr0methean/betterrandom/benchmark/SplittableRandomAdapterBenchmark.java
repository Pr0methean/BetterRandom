package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.concurrent.SplittableRandomAdapter;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.DEFAULT_INSTANCE;

public class SplittableRandomAdapterBenchmark extends AbstractRandomBenchmark {

  @Override protected Random createPrng() throws SeedException {
    return new SplittableRandomAdapter(DEFAULT_INSTANCE);
  }
}
