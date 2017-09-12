package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.util.Random;

public class AesCounterRandom256Benchmark extends AbstractRandomBenchmarkWithReseeding {

  @EntryPoint
  public AesCounterRandom256Benchmark() {
  }

  @Override
  protected Random createPrng()
      throws SeedException {
    return new AesCounterRandom(32);
  }
}
