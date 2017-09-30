package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.checkStream;

import io.github.pr0methean.betterrandom.seed.SeedException;
import org.testng.annotations.Test;

/**
 * Temporary workaround for https://github.com/cbeust/testng/issues/1563
 */
public class AesCounterTestNgBug1563WorkaroundTest {

  @Test
  public void testRenamedLongs3() throws Exception {
    BaseRandom result;
    try {
      result = new AesCounterRandom(16);
    } catch (final SeedException e) {
      throw new RuntimeException(e);
    }
    final BaseRandom prng = result;
    checkStream(prng, 42, prng.longs(20, 1L << 40, 1L << 42), 20, 1L << 40, 1L << 42,
        false);
  }
}
