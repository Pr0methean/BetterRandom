package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.Pcg64Random;
import io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;

/**
 * Benchmark for {@link Pcg64Random}.
 */
public class Pcg64RandomBenchmark extends AbstractRandomBenchmarkWithReseeding<Pcg64Random> {

  @Override protected Pcg64Random createPrng() throws SeedException {
    return new Pcg64Random(SecureRandomSeedGenerator.DEFAULT_INSTANCE);
  }
}
