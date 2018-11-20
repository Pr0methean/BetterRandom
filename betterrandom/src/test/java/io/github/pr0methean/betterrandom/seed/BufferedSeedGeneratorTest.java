package io.github.pr0methean.betterrandom.seed;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(singleThreaded = true)
public class BufferedSeedGeneratorTest extends AbstractSeedGeneratorTest {

  private static final FakeSeedGenerator FAKE_SEED_GENERATOR = new FakeSeedGenerator();
  private static final int BUFFER_SIZE = 256;

  private void generateAndCheckSeed(int length) {
    byte[] seed = seedGenerator.generateSeed(length);
    assertEquals(seed.length, length);
    for (int i = 0; i < length; i++) {
      assertEquals(seed[i], 1, "BufferedSeedGenerator failed to populate index " + i);
    }
  }

  public BufferedSeedGeneratorTest() {
    super(null);
  }

  @BeforeMethod
  public void setUp() {
    seedGenerator = new BufferedSeedGenerator(FAKE_SEED_GENERATOR, BUFFER_SIZE);
    FAKE_SEED_GENERATOR.reset();
  }

  @Test
  public void testLargeRequestDoneAsOne() {
    generateAndCheckSeed(2 * BUFFER_SIZE);
    assertEquals(FAKE_SEED_GENERATOR.countCalls(), 1);
    generateAndCheckSeed(BUFFER_SIZE / 2);
    assertEquals(FAKE_SEED_GENERATOR.countCalls(), 2);
  }

  @Test
  public void testSmallRequests() {
    generateAndCheckSeed(BUFFER_SIZE / 2);
    assertEquals(FAKE_SEED_GENERATOR.countCalls(), 1);
    generateAndCheckSeed(BUFFER_SIZE / 2);
    assertEquals(FAKE_SEED_GENERATOR.countCalls(), 1);
    generateAndCheckSeed(BUFFER_SIZE / 2);
    assertEquals(FAKE_SEED_GENERATOR.countCalls(), 2);
  }
}
