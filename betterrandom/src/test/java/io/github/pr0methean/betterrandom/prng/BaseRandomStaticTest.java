package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.prng.BaseRandom.entropyOfInt;
import static io.github.pr0methean.betterrandom.prng.BaseRandom.entropyOfLong;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Tests of the static methods of {@link BaseRandom}. In a separate class to work around
 * https://github.com/cbeust/testng/issues/1561
 */
public class BaseRandomStaticTest {
  @Test
  public static void testEntropyOfInt() {
    assertEquals(entropyOfInt(0, 1), 0);
    assertEquals(entropyOfInt(0, 2), 1);
    assertEquals(entropyOfInt(0, 1 << 24), 24);
    assertEquals(entropyOfInt(1 << 22, 1 << 24), 24);
    assertEquals(entropyOfInt(-(1 << 24), 0), 24);
    assertEquals(entropyOfInt(-(1 << 24), 1), 25);
  }

  @Test
  public static void testEntropyOfLong() {
    assertEquals(entropyOfLong(0, 1), 0);
    assertEquals(entropyOfLong(0, 2), 1);
    assertEquals(entropyOfLong(0, 1L << 42), 42);
    assertEquals(entropyOfLong(1 << 22, 1L << 42), 42);
    assertEquals(entropyOfLong(-(1L << 42), 0), 42);
    assertEquals(entropyOfLong(-(1L << 42), 1), 43);
  }
}
