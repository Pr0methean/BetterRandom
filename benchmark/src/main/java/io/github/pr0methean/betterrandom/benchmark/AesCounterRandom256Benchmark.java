package io.github.pr0methean.betterrandom.benchmark;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR;

import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;

public class AesCounterRandom256Benchmark extends AbstractRandomBenchmarkWithReseeding {

  @Override protected Random createPrng() throws SeedException {
    return new AesCounterRandom(SECURE_RANDOM_SEED_GENERATOR.generateSeed(32));
  }
}
