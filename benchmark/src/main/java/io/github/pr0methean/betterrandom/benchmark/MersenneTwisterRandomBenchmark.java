package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.MersenneTwisterRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.util.Random;

public class MersenneTwisterRandomBenchmark extends AbstractRandomBenchmarkWithReseeding {

  @EntryPoint
  public MersenneTwisterRandomBenchmark() {
  }

  @Override
  protected Random createPrng()
      throws SeedException {
    return new MersenneTwisterRandom();
  }
}
