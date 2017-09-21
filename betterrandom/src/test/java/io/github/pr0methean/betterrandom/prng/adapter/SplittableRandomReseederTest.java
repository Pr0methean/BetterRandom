package io.github.pr0methean.betterrandom.prng.adapter;

import static org.testng.Assert.*;

import io.github.pr0methean.betterrandom.util.BinaryUtils;
import java.util.Arrays;
import java.util.SplittableRandom;
import org.testng.annotations.Test;

public class SplittableRandomReseederTest {

  private static final long TEST_SEED = 0x0123456789ABCDEFL;

  @Test
  public void testReseed() throws Exception {
    long[] expected = new SplittableRandom(TEST_SEED).longs(20).toArray();
    SplittableRandom random = new SplittableRandom();
    random.nextLong();
    random = SplittableRandomReseeder.reseed(random, TEST_SEED);
    long[] actual = random.longs(20).toArray();
    assertTrue(Arrays.equals(expected, actual));
  }

  @Test
  public void testGetSeed() throws Exception {
    SplittableRandom random = new SplittableRandom(TEST_SEED);
    assertEquals(BinaryUtils.convertBytesToLong(SplittableRandomReseeder.getSeed(random)), TEST_SEED);
  }
}