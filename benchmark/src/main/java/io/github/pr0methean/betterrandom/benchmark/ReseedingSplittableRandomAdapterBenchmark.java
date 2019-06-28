package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR;

public class ReseedingSplittableRandomAdapterBenchmark extends AbstractRandomBenchmark {

  private final RandomSeederThread thread = new RandomSeederThread(SECURE_RANDOM_SEED_GENERATOR);

  @Override protected Random createPrng() throws SeedException {
    return ReseedingSplittableRandomAdapter.getInstance(thread, SECURE_RANDOM_SEED_GENERATOR);
  }
}
