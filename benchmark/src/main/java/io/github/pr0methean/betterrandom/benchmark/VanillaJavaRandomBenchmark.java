package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class VanillaJavaRandomBenchmark extends AbstractRandomBenchmarkWithReseeding {

  @Override
  protected Random createPrng(@UnknownInitialization VanillaJavaRandomBenchmark this)
      throws SeedException {
    return new Random();
  }
}
