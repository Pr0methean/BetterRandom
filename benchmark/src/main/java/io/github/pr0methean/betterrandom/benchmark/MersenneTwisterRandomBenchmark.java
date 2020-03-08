package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.MersenneTwisterRandom;
import io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;

public class MersenneTwisterRandomBenchmark extends AbstractRandomBenchmarkWithReseeding<MersenneTwisterRandom> {

  @Override protected MersenneTwisterRandom createPrng() throws SeedException {
    return new MersenneTwisterRandom(SecureRandomSeedGenerator.DEFAULT_INSTANCE);
  }
}
