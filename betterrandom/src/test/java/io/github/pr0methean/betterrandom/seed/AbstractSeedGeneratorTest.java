package io.github.pr0methean.betterrandom.seed;

import static org.testng.Assert.assertEquals;

import org.testng.Assert;
import org.testng.annotations.Test;

public abstract class AbstractSeedGeneratorTest {

  protected SeedGenerator seedGenerator;

  protected AbstractSeedGeneratorTest(final SeedGenerator seedGenerator) {
    this.seedGenerator = seedGenerator;
  }

  @Test public void testToString() {
    Assert.assertNotNull(seedGenerator.toString(),
        "toString() returned null for a " + seedGenerator.getClass().getSimpleName());
  }

  /**
   * Tests seed generation that should delegate to a {@link FakeSeedGenerator}.
   *
   * @param length the length of the seed to generate
   */
  protected void generateAndCheckFakeSeed(int length) {
    byte[] seed = seedGenerator.generateSeed(length);
    assertEquals(seed.length, length, "Wrong seed length");
    for (int i = 0; i < length; i++) {
      assertEquals(seed[i], 1, "BufferedSeedGenerator failed to populate index " + i);
    }
  }
}
