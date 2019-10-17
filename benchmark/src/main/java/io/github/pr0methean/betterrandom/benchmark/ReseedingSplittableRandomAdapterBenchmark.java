package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.prng.concurrent.ReseedingSplittableRandomAdapter;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.DEFAULT_INSTANCE;

public class ReseedingSplittableRandomAdapterBenchmark extends AbstractRandomBenchmark {

  private final RandomSeederThread thread = new RandomSeederThread(DEFAULT_INSTANCE);

  @Override protected Random createPrng() throws SeedException {
    return ReseedingSplittableRandomAdapter.getInstance(thread, DEFAULT_INSTANCE);
  }
}
