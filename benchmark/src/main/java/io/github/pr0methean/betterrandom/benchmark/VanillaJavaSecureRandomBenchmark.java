package io.github.pr0methean.betterrandom.benchmark;

import java.security.SecureRandom;
import java.util.Random;

public class VanillaJavaSecureRandomBenchmark extends AbstractRandomBenchmark {

  @Override protected Random createPrng() throws Exception {
    return SecureRandom.getInstance("SHA1PRNG");
  }
}
