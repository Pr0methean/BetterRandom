package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.adapter.RandomWrapper;
import io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;

public class RandomWrapperBenchmark extends AbstractRandomBenchmarkWithReseeding<RandomWrapper<Random>> {

  @Override protected RandomWrapper<Random> createPrng() throws SeedException {
    return RandomWrapper.wrapJavaUtilRandom(SecureRandomSeedGenerator.DEFAULT_INSTANCE);
  }
}
