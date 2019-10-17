package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.concurrent.ThreadLocalRandomWrapper;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import java8.util.function.Function;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.DEFAULT_INSTANCE;

public class ThreadLocalRandomWrapperAesCounterRandom128Benchmark extends AbstractRandomBenchmark {

  @Override protected Random createPrng() throws SeedException {
    return new ThreadLocalRandomWrapper(16, DEFAULT_INSTANCE,
        new Function<byte[], BaseRandom>() {
          @Override public BaseRandom apply(byte[] seed) {
            return new AesCounterRandom(seed);
          }
        });
  }
}
