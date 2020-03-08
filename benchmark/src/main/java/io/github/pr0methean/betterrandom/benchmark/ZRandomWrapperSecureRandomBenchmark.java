package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.adapter.RandomWrapper;
import java.security.SecureRandom;
import java.util.Random;

public class ZRandomWrapperSecureRandomBenchmark extends AbstractRandomBenchmark {

  @Override protected Random createPrng() throws Exception {
    return new RandomWrapper<Random>(SecureRandom.getInstance("SHA1PRNG"));
  }
}
