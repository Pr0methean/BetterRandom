package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.Cmwc4096Random;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.util.Random;

public class Cmwc4096RandomBenchmark extends AbstractRandomBenchmarkWithReseeding {

  @EntryPoint
  public Cmwc4096RandomBenchmark() {
  }

  @Override
  protected Random createPrng()
      throws SeedException {
    return new Cmwc4096Random();
  }
}
