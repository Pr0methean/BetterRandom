package io.github.pr0methean.betterrandom.benchmark;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

public class ZVanillaJavaSecureRandomBenchmark extends BenchmarkWithLegacyRandomSeeder {

  @Override protected Random createPrng() {
    try {
      return SecureRandom.getInstance("SHA1PRNG");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
