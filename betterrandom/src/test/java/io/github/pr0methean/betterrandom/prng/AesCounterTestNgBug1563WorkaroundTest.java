package io.github.pr0methean.betterrandom.prng;

/**
 * Temporary workaround for https://github.com/cbeust/testng/issues/1563
 */
public class AesCounterTestNgBug1563WorkaroundTest extends AesCounterRandom128Test {

  public void testLongs3_() throws Exception {
    super.testLongs3();
  }
}
