package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.JavaRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;

public class DecoratedJavaRandomBenchmark extends AbstractRandomBenchmarkWithReseeding {

  @Override
  protected Random createPrng()
      throws SeedException {
    return new JavaRandom();
  }
}
