package io.github.pr0methean.betterrandom.benchmark;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.DEFAULT_INSTANCE;

import io.github.pr0methean.betterrandom.prng.adapter.ReseedingSplittableRandomAdapter;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeeder;
import java.util.Random;

public class ReseedingSplittableRandomAdapterBenchmark extends AbstractRandomBenchmark {

  private final SimpleRandomSeeder thread = new SimpleRandomSeeder(DEFAULT_INSTANCE);

  @Override protected Random createPrng() throws SeedException {
    return ReseedingSplittableRandomAdapter.getInstance(thread, DEFAULT_INSTANCE);
  }
}
