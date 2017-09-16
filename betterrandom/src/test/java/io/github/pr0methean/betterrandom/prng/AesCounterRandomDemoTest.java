package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedException;
import org.testng.annotations.Test;

public class AesCounterRandomDemoTest {

  private static final String[] NO_ARGS = {};

  @Test
  public void ensureNoDemoCrash() throws SeedException {
    AesCounterRandomDemo.main(NO_ARGS);
  }
}
