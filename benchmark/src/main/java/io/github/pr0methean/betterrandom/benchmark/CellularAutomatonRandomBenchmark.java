package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.CellularAutomatonRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class CellularAutomatonRandomBenchmark extends AbstractRandomBenchmark {

  @Override
  protected Random createPrng(@UnknownInitialization CellularAutomatonRandomBenchmark this) throws SeedException {
    return new CellularAutomatonRandom();
  }
}
