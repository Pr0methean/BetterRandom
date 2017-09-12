package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.XorShiftRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.util.Random;

public class XorShiftRandomBenchmark extends AbstractRandomBenchmarkWithReseeding {

  @EntryPoint
  public XorShiftRandomBenchmark() {
  }

  @Override
  protected Random createPrng()
      throws SeedException {
    return new XorShiftRandom();
  }
}
