package io.github.pr0methean.betterrandom.seed;

import static org.testng.Assert.assertFalse;

import java.util.Arrays;

enum SeedTestUtils {
  ;

  public static final int SEED_SIZE = 16;
  @SuppressWarnings("MismatchedReadAndWriteOfArray") private static final byte[] ALL_ZEROES =
      new byte[SEED_SIZE];

  public static void testGenerator(final SeedGenerator seedGenerator, boolean expectNonIdempotent) {
    testGenerator(seedGenerator, expectNonIdempotent, SEED_SIZE);
  }

  public static void testGenerator(final SeedGenerator seedGenerator, boolean expectNonIdempotent,
      int seedSize) {
    final byte[] seed = seedGenerator.generateSeed(seedSize);
    assert seed.length == seedSize : "Failed to generate seed of correct length";
    assertFalse(Arrays.equals(seed, ALL_ZEROES), "Generated an all-zeroes seed");
    if (expectNonIdempotent) {
      final byte[] secondSeed = new byte[seedSize];
      seedGenerator.generateSeed(secondSeed); // Check that other syntax also works
      assertFalse(Arrays.equals(secondSeed, ALL_ZEROES));
      assertFalse(Arrays.equals(seed, secondSeed));
    }
  }
}
