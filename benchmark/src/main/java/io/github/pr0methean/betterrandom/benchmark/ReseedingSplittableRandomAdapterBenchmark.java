package io.github.pr0methean.betterrandom.benchmark;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.DEFAULT_INSTANCE;

import io.github.pr0methean.betterrandom.prng.concurrent.ReseedingSplittableRandomAdapter;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeederThread;
import java.util.Random;

public class ReseedingSplittableRandomAdapterBenchmark extends AbstractRandomBenchmark {

  private final SimpleRandomSeederThread thread = new RandomSeederThread(DEFAULT_INSTANCE);

  @Override protected Random createPrng() throws SeedException {
    return ReseedingSplittableRandomAdapter.getInstance(thread, DEFAULT_INSTANCE);
  }
}
