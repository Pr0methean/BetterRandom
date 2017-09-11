package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.adapter.ReseedingSplittableRandomAdapter;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;

public class ReseedingSplittableRandomAdapterBenchmark extends
    AbstractRandomBenchmark {

  @Override
  protected Random createPrng()
      throws SeedException {
    return ReseedingSplittableRandomAdapter.getDefaultInstance();
  }
}
