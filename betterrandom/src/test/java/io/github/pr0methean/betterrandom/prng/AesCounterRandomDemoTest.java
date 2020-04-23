package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedException;
import org.powermock.api.mockito.mockpolicies.Slf4jMockPolicy;
import org.powermock.core.classloader.annotations.MockPolicy;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.Test;

@MockPolicy(Slf4jMockPolicy.class)
@Test(testName = "AesCounterRandomDemo")
public class AesCounterRandomDemoTest extends PowerMockTestCase {

  private static final String[] NO_ARGS = {};

  @Test(timeOut = 120_000) public void ensureNoDemoCrash() throws SeedException {
    AesCounterRandomDemo.main(NO_ARGS);
  }
}
