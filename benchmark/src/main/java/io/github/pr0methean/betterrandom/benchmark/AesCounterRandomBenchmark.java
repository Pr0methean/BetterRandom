package io.github.pr0methean.betterrandom.benchmark;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.DEFAULT_INSTANCE;

import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import org.openjdk.jmh.annotations.Param;

/**
 * The benchmark for {@link AesCounterRandom}.
 */
public class AesCounterRandomBenchmark extends AbstractRandomBenchmarkWithReseeding {

  /**
   * The initial seed size.
   */
  @Param({"16", "32"}) public int seedSize;

  @Override protected Random createPrng() throws SeedException {
    return new AesCounterRandom(DEFAULT_INSTANCE.generateSeed(seedSize));
  }
}
