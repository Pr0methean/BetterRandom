package io.github.pr0methean.betterrandom.benchmark;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR;

import io.github.pr0methean.betterrandom.prng.XorShiftRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;

public class XorShiftRandomBenchmark extends AbstractRandomBenchmarkWithReseeding {

  @Override protected Random createPrng() throws SeedException {
    return new XorShiftRandom(SECURE_RANDOM_SEED_GENERATOR);
  }
}
