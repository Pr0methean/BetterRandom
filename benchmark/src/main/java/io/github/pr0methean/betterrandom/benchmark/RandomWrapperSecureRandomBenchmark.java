package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.RandomWrapper;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.security.SecureRandom;
import java.util.Random;

public class RandomWrapperSecureRandomBenchmark extends AbstractRandomBenchmark {

  @EntryPoint
  public RandomWrapperSecureRandomBenchmark() {
  }

  @Override
  protected Random createPrng() throws SeedException {
    return new RandomWrapper(new SecureRandom());
  }
}
