package io.github.pr0methean.betterrandom.seed;

import static org.testng.Assert.assertEquals;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.powermock.api.mockito.mockpolicies.Slf4jMockPolicy;
import org.powermock.core.classloader.annotations.MockPolicy;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@MockPolicy(Slf4jMockPolicy.class)
public abstract class SeedGeneratorTest<T extends SeedGenerator> extends PowerMockTestCase {

  protected T seedGenerator;

  protected abstract T initializeSeedGenerator();

  @BeforeMethod public void setUp() {
    seedGenerator = initializeSeedGenerator();
  }

  @AfterMethod public void tearDown() {
    seedGenerator = null;
  }

  @Test public void testToString() {
    Assert.assertNotNull(seedGenerator.toString(),
        "toString() returned null for a " + seedGenerator.getClass().getSimpleName());
  }

  @Test public void testWithEqualsVerifier() {
    EqualsVerifier.forClass(seedGenerator.getClass()).verify();
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
