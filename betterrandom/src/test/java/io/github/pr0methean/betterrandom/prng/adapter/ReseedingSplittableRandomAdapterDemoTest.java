package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.seed.SeedException;
import org.testng.annotations.Test;

public class ReseedingSplittableRandomAdapterDemoTest {

  private static final String[] NO_ARGS = {};

  @Test(timeOut = 120_000) public void ensureNoDemoCrash()
      throws SeedException, InterruptedException {
    ReseedingSplittableRandomAdapterDemo.main(NO_ARGS);
  }
}
