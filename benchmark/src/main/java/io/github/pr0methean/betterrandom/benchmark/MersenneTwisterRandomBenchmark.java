package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.MersenneTwisterRandom;
import io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;

public class MersenneTwisterRandomBenchmark extends AbstractRandomBenchmarkWithReseeding {

  @Override protected Random createPrng() throws SeedException {
    return new MersenneTwisterRandom(SecureRandomSeedGenerator.DEFAULT_INSTANCE);
  }
}
