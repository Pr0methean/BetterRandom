package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedException;
import org.testng.annotations.Test;

@Test(testName = "AesCounterRandomDemo") public class AesCounterRandomDemoTest {

  private static final String[] NO_ARGS = {};

  @Test(timeOut = 120_000) public void ensureNoDemoCrash() throws SeedException {
    AesCounterRandomDemo.main(NO_ARGS);
  }
}
