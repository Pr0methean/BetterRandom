package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class AesCounterRandom128Benchmark extends AbstractRandomBenchmarkWithReseeding {

  public AesCounterRandom128Benchmark() {
  }

  @Override
  protected Random createPrng(@UnknownInitialization AesCounterRandom128Benchmark this)
      throws SeedException {
    return new AesCounterRandom(16);
  }
}
