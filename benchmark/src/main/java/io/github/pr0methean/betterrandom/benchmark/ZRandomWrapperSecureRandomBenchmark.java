package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.adapter.RandomWrapper;
import java.security.SecureRandom;

public class ZRandomWrapperSecureRandomBenchmark extends AbstractRandomBenchmark<RandomWrapper<SecureRandom>> {

  @Override protected RandomWrapper<SecureRandom> createPrng() throws Exception {
    return new RandomWrapper<>(SecureRandom.getInstance("SHA1PRNG"));
  }
}
