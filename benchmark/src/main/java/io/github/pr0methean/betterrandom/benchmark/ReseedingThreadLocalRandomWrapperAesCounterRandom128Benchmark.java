package io.github.pr0methean.betterrandom.benchmark;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR;

import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.prng.adapter.ReseedingThreadLocalRandomWrapper;
import java.util.Random;

public class ReseedingThreadLocalRandomWrapperAesCounterRandom128Benchmark
    extends AbstractRandomBenchmark {

  @Override protected Random createPrng() {
    return new ReseedingThreadLocalRandomWrapper(SECURE_RANDOM_SEED_GENERATOR,
        () -> new AesCounterRandom(SECURE_RANDOM_SEED_GENERATOR.generateSeed(16)));
  }
}
