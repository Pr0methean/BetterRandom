package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.security.SecureRandom;
import java.util.Random;

public class VanillaJavaSecureRandomBenchmark extends AbstractRandomBenchmark {

  @EntryPoint
  public VanillaJavaSecureRandomBenchmark() {}

  @Override
  protected Random createPrng()
      throws SeedException {
    return new SecureRandom();
  }
}
