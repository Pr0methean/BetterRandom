package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.RandomWrapper;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.util.Random;

public class RandomWrapperBenchmark extends AbstractRandomBenchmarkWithReseeding {

  @EntryPoint
  public RandomWrapperBenchmark() {}

  @Override
  protected Random createPrng()
      throws SeedException {
    return new RandomWrapper();
  }
}
