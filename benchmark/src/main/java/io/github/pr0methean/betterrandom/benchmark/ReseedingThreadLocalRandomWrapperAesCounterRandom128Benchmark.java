package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.prng.concurrent.ReseedingThreadLocalRandomWrapper;
import java.util.Random;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR;

public class ReseedingThreadLocalRandomWrapperAesCounterRandom128Benchmark
    extends AbstractRandomBenchmark {

  @Override protected Random createPrng() {
    return new ReseedingThreadLocalRandomWrapper(SECURE_RANDOM_SEED_GENERATOR,
        () -> new AesCounterRandom(SECURE_RANDOM_SEED_GENERATOR.generateSeed(16)));
  }
}
