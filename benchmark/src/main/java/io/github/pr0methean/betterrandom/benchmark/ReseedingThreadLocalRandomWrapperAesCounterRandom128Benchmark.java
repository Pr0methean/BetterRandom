package io.github.pr0methean.betterrandom.benchmark;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.DEFAULT_INSTANCE;

import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.prng.adapter.ReseedingThreadLocalRandomWrapper;
import java.util.Random;

/**
 * Benchmark for {@link ReseedingThreadLocalRandomWrapper} wrapping {@link AesCounterRandom}.
 */
public class ReseedingThreadLocalRandomWrapperAesCounterRandom128Benchmark
    extends AbstractRandomBenchmark<Random> {

  @Override protected ReseedingThreadLocalRandomWrapper<AesCounterRandom> createPrng() {
    return new ReseedingThreadLocalRandomWrapper<>(DEFAULT_INSTANCE,
        () -> new AesCounterRandom(DEFAULT_INSTANCE.generateSeed(16)));
  }
}
