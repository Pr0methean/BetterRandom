package io.github.pr0methean.betterrandom.seed;

import static org.testng.Assert.assertFalse;

import java.util.Arrays;

public final class SeedTestUtils {
  private static final int SEED_SIZE = 32;
  private static final byte[] ALL_ZEROES = new byte[SEED_SIZE];
  private SeedTestUtils() {}

  public static void testGenerator(SeedGenerator seedGenerator) throws SeedException {
    final SeedGenerator generator = seedGenerator;
    final byte[] seed = generator.generateSeed(SEED_SIZE);
    assert seed.length == SEED_SIZE : "Failed to generate seed of correct length";
    assertFalse(Arrays.equals(seed, ALL_ZEROES));
    final byte[] secondSeed = new byte[SEED_SIZE];
    generator.generateSeed(secondSeed); // Check that other syntax also works
    assertFalse(Arrays.equals(secondSeed, ALL_ZEROES));
    assertFalse(Arrays.equals(seed, secondSeed));
  }
}
