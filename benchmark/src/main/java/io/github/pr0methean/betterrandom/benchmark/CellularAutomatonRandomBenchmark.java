package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.CellularAutomatonRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.util.Random;

public class CellularAutomatonRandomBenchmark extends AbstractRandomBenchmarkWithReseeding {

  @EntryPoint
  public CellularAutomatonRandomBenchmark() {
  }

  @Override
  protected Random createPrng()
      throws SeedException {
    return new CellularAutomatonRandom();
  }
}
