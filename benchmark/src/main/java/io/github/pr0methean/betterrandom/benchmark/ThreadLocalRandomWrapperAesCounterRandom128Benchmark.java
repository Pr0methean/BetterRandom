package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.prng.ThreadLocalRandomWrapper;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;

public class ThreadLocalRandomWrapperAesCounterRandom128Benchmark extends AbstractRandomBenchmark {

  @Override
  protected Random createPrng() throws SeedException {
    return new ThreadLocalRandomWrapper(16, DefaultSeedGenerator.DEFAULT_SEED_GENERATOR,
        AesCounterRandom::new);
  }
}
