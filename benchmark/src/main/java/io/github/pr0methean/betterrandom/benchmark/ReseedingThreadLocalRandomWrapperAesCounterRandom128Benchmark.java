package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.prng.concurrent.ReseedingThreadLocalRandomWrapper;
import java.util.Random;
import java8.util.function.Supplier;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.DEFAULT_INSTANCE;

public class ReseedingThreadLocalRandomWrapperAesCounterRandom128Benchmark
    extends AbstractRandomBenchmark {

  @Override protected Random createPrng() {
    return new ReseedingThreadLocalRandomWrapper(DEFAULT_INSTANCE,
        new Supplier<AesCounterRandom>() {
          @Override public AesCounterRandom get() {
            return new AesCounterRandom(DEFAULT_INSTANCE.generateSeed(16));
          }
        });
  }
}
