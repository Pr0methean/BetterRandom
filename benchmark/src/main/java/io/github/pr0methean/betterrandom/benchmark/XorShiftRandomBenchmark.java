package io.github.pr0methean.betterrandom.benchmark;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.DEFAULT_INSTANCE;

import io.github.pr0methean.betterrandom.prng.XorShiftRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;

public class XorShiftRandomBenchmark extends AbstractRandomBenchmarkWithReseeding<XorShiftRandom> {

  @Override protected XorShiftRandom createPrng() throws SeedException {
    return new XorShiftRandom(DEFAULT_INSTANCE);
  }
}
