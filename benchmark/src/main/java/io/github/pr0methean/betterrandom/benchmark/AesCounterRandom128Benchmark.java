package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;

public class AesCounterRandom128Benchmark extends AbstractRandomBenchmark {

  protected AesCounterRandom128Benchmark() throws SeedException {
  }

  @Override
  protected Random createPrng() throws SeedException {
    return new AesCounterRandom(16);
  }
}
