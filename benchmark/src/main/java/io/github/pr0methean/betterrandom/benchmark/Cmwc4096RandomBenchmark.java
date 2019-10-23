package io.github.pr0methean.betterrandom.benchmark;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.DEFAULT_INSTANCE;

import io.github.pr0methean.betterrandom.prng.Cmwc4096Random;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;

/**
 * The benchmark for {@link Cmwc4096Random}.
 */
public class Cmwc4096RandomBenchmark extends AbstractRandomBenchmarkWithReseeding {

  @Override protected Random createPrng() throws SeedException {
    return new Cmwc4096Random(DEFAULT_INSTANCE);
  }
}
