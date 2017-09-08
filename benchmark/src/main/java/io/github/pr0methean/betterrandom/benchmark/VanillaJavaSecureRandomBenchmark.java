package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.seed.SeedException;
import java.security.SecureRandom;
import java.util.Random;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class VanillaJavaSecureRandomBenchmark extends AbstractRandomBenchmark {

  @Override
  protected Random createPrng(@UnknownInitialization VanillaJavaSecureRandomBenchmark this)
      throws SeedException {
    return new SecureRandom();
  }
}
