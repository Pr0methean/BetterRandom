package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.checkStream;

import org.testng.annotations.Test;

/**
 * Temporary workaround for https://github.com/cbeust/testng/issues/1563
 */
public class AesCounterTestNgBug1563WorkaroundTest extends AesCounterRandom128Test {

  @Test
  public void testLongs3_() throws Exception {
    super.testLongs3();
  }
}
