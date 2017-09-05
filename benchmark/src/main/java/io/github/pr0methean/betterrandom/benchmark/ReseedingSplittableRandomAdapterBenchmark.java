package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.adapter.ReseedingSplittableRandomAdapter;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class ReseedingSplittableRandomAdapterBenchmark extends AbstractRandomBenchmark {

  @Override
  protected Random createPrng(@UnknownInitialization ReseedingSplittableRandomAdapterBenchmark this) throws SeedException {
    return ReseedingSplittableRandomAdapter.getDefaultInstance();
  }
}
