package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.Cmwc4096Random;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class Cmwc4096RandomBenchmark extends AbstractRandomBenchmark {

  @Override
  protected Random createPrng(@UnknownInitialization Cmwc4096RandomBenchmark this) throws SeedException {
    return new Cmwc4096Random();
  }
}
