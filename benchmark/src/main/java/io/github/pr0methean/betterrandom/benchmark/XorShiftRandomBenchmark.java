package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.XorShiftRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class XorShiftRandomBenchmark extends AbstractRandomBenchmarkWithReseeding {

  @Override
  protected Random createPrng(@UnknownInitialization XorShiftRandomBenchmark this)
      throws SeedException {
    return new XorShiftRandom();
  }
}
