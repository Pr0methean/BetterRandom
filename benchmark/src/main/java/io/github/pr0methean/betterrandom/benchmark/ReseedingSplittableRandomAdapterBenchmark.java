package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.adapter.ReseedingSplittableRandomAdapter;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.util.Random;

public class ReseedingSplittableRandomAdapterBenchmark extends
    AbstractRandomBenchmark {

  @EntryPoint
  public ReseedingSplittableRandomAdapterBenchmark() {}

  @Override
  protected Random createPrng()
      throws SeedException {
    return ReseedingSplittableRandomAdapter.getDefaultInstance();
  }
}
