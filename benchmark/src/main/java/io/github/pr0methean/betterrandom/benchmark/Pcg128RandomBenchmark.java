package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.Pcg128Random;
import io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;

public class Pcg128RandomBenchmark extends AbstractRandomBenchmarkWithReseeding<Pcg128Random> {

  @Override protected Pcg128Random createPrng() throws SeedException {
    return new Pcg128Random(SecureRandomSeedGenerator.DEFAULT_INSTANCE);
  }
}
