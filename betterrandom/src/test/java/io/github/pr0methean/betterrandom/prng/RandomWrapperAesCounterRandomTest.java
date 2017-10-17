package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedException;
import java.lang.reflect.InvocationTargetException;
import org.testng.annotations.Test;

public class RandomWrapperAesCounterRandomTest extends AesCounterRandom128Test {

  @Override
  protected Class<? extends BaseRandom> getClassUnderTest() {
    return RandomWrapper.class;
  }

  @Override
  @Test(enabled = false)
  public void testAllPublicConstructors()
      throws SeedException, IllegalAccessException, InstantiationException, InvocationTargetException {
    // No-op: redundant to super insofar as it works.
  }

  @Override
  protected RandomWrapper createRng() throws SeedException {
    return new RandomWrapper(new AesCounterRandom());
  }

  @Override
  protected RandomWrapper createRng(final byte[] seed) throws SeedException {
    return new RandomWrapper(new AesCounterRandom(seed));
  }
}
