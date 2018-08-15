package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.seed.SeedException;
import java.security.SecureRandom;
import java.util.Random;

public class VanillaJavaSecureRandomBenchmark extends AbstractRandomBenchmark {

  @Override protected Random createPrng() throws SeedException {
    return SecureRandom.getInstance("SHA1PRNG");
  }
}
