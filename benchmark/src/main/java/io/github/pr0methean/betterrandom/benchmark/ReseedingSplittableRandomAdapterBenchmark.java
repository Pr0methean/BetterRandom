package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.adapter.ReseedingSplittableRandomAdapter;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR;

public class ReseedingSplittableRandomAdapterBenchmark extends AbstractRandomBenchmark {

  @Override protected Random createPrng() throws SeedException {
    return ReseedingSplittableRandomAdapter.getInstance(SECURE_RANDOM_SEED_GENERATOR);
  }
}
