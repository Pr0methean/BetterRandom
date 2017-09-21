package io.github.pr0methean.betterrandom.prng.adapter;

import static io.github.pr0methean.betterrandom.prng.adapter.SplittableRandomReseeder.SPLITTABLE_RANDOM_CLASS;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import io.github.pr0methean.betterrandom.TestingDeficiency;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import java.util.Arrays;
import java.util.SplittableRandom;
import org.testng.Assert;
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

  @TestingDeficiency
  @Test(enabled = false) // https://github.com/Pr0methean/BetterRandom/issues/5
  public void testGetSeed() throws Exception {
    SplittableRandom random = new SplittableRandom(TEST_SEED);
    // Test for class-loader trickery by the test framework
    assertTrue(SPLITTABLE_RANDOM_CLASS.isInstance(random));
    assertEquals(BinaryUtils.convertBytesToLong(SplittableRandomReseeder.getSeed(random)),
        TEST_SEED);
  }
}