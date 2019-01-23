package io.github.pr0methean.betterrandom.prng.concurrent;

import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import org.testng.annotations.Test;

public class ReseedingSplittableRandomAdapterDemoTest {

  private static final String[] NO_ARGS = {};

  @Test(timeOut = 120_000) public void ensureNoDemoCrash()
      throws SeedException, InterruptedException {
    try {
      ReseedingSplittableRandomAdapterDemo.main(NO_ARGS);
    } finally {
      RandomSeederThread.clear(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
      RandomSeederThread.stopAllEmpty();
    }
  }
}
