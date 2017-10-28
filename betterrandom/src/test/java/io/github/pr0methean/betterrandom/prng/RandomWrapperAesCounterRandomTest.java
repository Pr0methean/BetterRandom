package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedException;
import org.testng.annotations.Test;

public class RandomWrapperAesCounterRandomTest extends AesCounterRandom128Test {


  @Override @Test(enabled = false) public void testAdvanceForward() {
    // No-op: RandomWrapper isn't seekable
  }

  @Override @Test(enabled = false) public void testAdvanceBackward() {
    // No-op: RandomWrapper isn't seekable
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return RandomWrapper.class;
  }

  @Override protected RandomWrapper createRng() throws SeedException {
    return new RandomWrapper(new AesCounterRandom());
  }

  @Override protected RandomWrapper createRng(final byte[] seed) throws SeedException {
    return new RandomWrapper(new AesCounterRandom(seed));
  }
}
