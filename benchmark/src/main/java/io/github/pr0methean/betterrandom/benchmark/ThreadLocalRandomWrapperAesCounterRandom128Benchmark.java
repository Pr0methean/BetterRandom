package io.github.pr0methean.betterrandom.benchmark;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.DEFAULT_INSTANCE;

import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.prng.adapter.ThreadLocalRandomWrapper;
import io.github.pr0methean.betterrandom.seed.SeedException;

public class ThreadLocalRandomWrapperAesCounterRandom128Benchmark
    extends AbstractRandomBenchmark<ThreadLocalRandomWrapper<AesCounterRandom>> {

  @Override protected ThreadLocalRandomWrapper<AesCounterRandom> createPrng() throws SeedException {
    return new ThreadLocalRandomWrapper<>(16, DEFAULT_INSTANCE,
        AesCounterRandom::new);
  }
}
