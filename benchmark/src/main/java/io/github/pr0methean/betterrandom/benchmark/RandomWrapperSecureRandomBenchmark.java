package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.RandomWrapper;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.security.SecureRandom;
import java.util.Random;

public class RandomWrapperSecureRandomBenchmark extends AbstractRandomBenchmark {

  @Override protected Random createPrng() throws SeedException {
    return new RandomWrapper(SecureRandom.getInstance("SHA1PRNG"));
  }
}
