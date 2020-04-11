package io.github.pr0methean.betterrandom.seed;

import static org.testng.Assert.assertEquals;

import javax.annotation.Nullable;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public abstract class SeedGeneratorTest<T extends SeedGenerator> extends PowerMockTestCase {

  protected T seedGenerator;

  @Nullable protected final T defaultSeedGenerator;

  protected SeedGeneratorTest(@Nullable final T seedGenerator) {
    this.defaultSeedGenerator = seedGenerator;
  }

  protected T getSeedGenerator() {
    return defaultSeedGenerator;
  }

  @BeforeMethod public void setUp() {
    seedGenerator = getSeedGenerator();
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
    generateAndCheckFakeSeed(length, 0);
  }

  /**
   * Tests seed generation that should delegate to a {@link FakeSeedGenerator}.
   *
   * @param length the length of the seed to generate
   * @param offset the expected value of byte 0
   */
  protected void generateAndCheckFakeSeed(int length, int offset) {
    byte[] seed = seedGenerator.generateSeed(length);
    assertEquals(seed.length, length, "Wrong seed length");
    for (int i = 0; i < length; i++) {
      assertEquals(seed[i], (byte) (i + offset), "BufferedSeedGenerator failed to populate index " + i);
    }
  }
}
