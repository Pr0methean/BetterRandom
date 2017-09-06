package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.MersenneTwisterRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class MersenneTwisterRandomBenchmark extends AbstractRandomBenchmark {

  @Override
  protected Random createPrng(@UnknownInitialization MersenneTwisterRandomBenchmark this) throws SeedException {
    return new MersenneTwisterRandom();
  }
}
