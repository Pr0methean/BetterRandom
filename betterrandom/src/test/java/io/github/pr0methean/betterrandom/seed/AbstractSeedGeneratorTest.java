package io.github.pr0methean.betterrandom.seed;

import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public abstract class AbstractSeedGeneratorTest extends PowerMockTestCase {

  protected SeedGenerator seedGenerator;

  protected AbstractSeedGeneratorTest(final SeedGenerator seedGenerator) {
    this.seedGenerator = seedGenerator;
  }

  @Test public void testToString() {
    Assert.assertNotNull(seedGenerator.toString());
  }

  protected void generateAndCheckFakeSeed(int length) {
    byte[] seed = seedGenerator.generateSeed(length);
    assertEquals(seed.length, length);
    for (int i = 0; i < length; i++) {
      assertEquals(seed[i], 1, "BufferedSeedGenerator failed to populate index " + i);
    }
  }
}
