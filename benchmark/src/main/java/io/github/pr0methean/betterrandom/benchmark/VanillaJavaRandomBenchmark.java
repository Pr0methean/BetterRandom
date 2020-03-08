package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;

public class VanillaJavaRandomBenchmark extends BenchmarkWithLegacyRandomSeeder<Random> {

  @Override protected Random createPrng() throws SeedException {
    return new Random();
  }
}
