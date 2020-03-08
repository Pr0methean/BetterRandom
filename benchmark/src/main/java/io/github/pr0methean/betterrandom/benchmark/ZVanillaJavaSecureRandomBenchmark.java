package io.github.pr0methean.betterrandom.benchmark;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class ZVanillaJavaSecureRandomBenchmark extends BenchmarkWithLegacyRandomSeeder<SecureRandom> {

  @Override protected SecureRandom createPrng() {
    try {
      return SecureRandom.getInstance("SHA1PRNG");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
