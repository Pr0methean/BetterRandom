package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.JavaRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class DecoratedJavaRandomBenchmark extends AbstractRandomBenchmarkWithReseeding {

  @Override
  protected Random createPrng(@UnknownInitialization DecoratedJavaRandomBenchmark this)
      throws SeedException {
    return new JavaRandom();
  }
}
