package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.util.Random;

public class VanillaJavaRandomBenchmark extends AbstractRandomBenchmarkWithReseeding {

  @EntryPoint
  public VanillaJavaRandomBenchmark() {}

  @Override
  protected Random createPrng()
      throws SeedException {
    return new Random();
  }
}
