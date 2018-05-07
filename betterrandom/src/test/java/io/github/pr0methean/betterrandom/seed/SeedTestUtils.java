package io.github.pr0methean.betterrandom.seed;

import static org.testng.Assert.assertFalse;

import java.util.Arrays;

enum SeedTestUtils {
  ;

  private static final int SEED_SIZE = 16;
  @SuppressWarnings("MismatchedReadAndWriteOfArray") private static final byte[] ALL_ZEROES =
      new byte[SEED_SIZE];

  public static void testGenerator(final SeedGenerator seedGenerator) throws SeedException {
    final byte[] seed = seedGenerator.generateSeed(SEED_SIZE);
    assert seed.length == SEED_SIZE : "Failed to generate seed of correct length";
    assertFalse(Arrays.equals(seed, ALL_ZEROES));
    final byte[] secondSeed = new byte[SEED_SIZE];
    seedGenerator.generateSeed(secondSeed); // Check that other syntax also works
    assertFalse(Arrays.equals(secondSeed, ALL_ZEROES));
    assertFalse(Arrays.equals(seed, secondSeed));
  }
}
