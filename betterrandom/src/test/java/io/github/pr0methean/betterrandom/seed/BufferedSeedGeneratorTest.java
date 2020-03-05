package io.github.pr0methean.betterrandom.seed;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(singleThreaded = true) public class BufferedSeedGeneratorTest
    extends SeedGeneratorTest<BufferedSeedGenerator> {

  private static final FakeSeedGenerator FAKE_SEED_GENERATOR = new FakeSeedGenerator();
  private static final int BUFFER_SIZE = 256;

  public BufferedSeedGeneratorTest() {
    super(null);
  }

  @BeforeMethod public void setUp() {
    seedGenerator = new BufferedSeedGenerator(FAKE_SEED_GENERATOR, BUFFER_SIZE);
    FAKE_SEED_GENERATOR.reset();
  }

  @Test public void testLargeRequestDoneAsOne() {
    generateAndCheckFakeSeed(2 * BUFFER_SIZE);
    assertEquals(FAKE_SEED_GENERATOR.countCalls(), 1);
    generateAndCheckFakeSeed(BUFFER_SIZE / 2);
    assertEquals(FAKE_SEED_GENERATOR.countCalls(), 2);
  }

  @Test public void testSmallRequests() {
    generateAndCheckFakeSeed(BUFFER_SIZE / 2);
    assertEquals(FAKE_SEED_GENERATOR.countCalls(), 1);
    generateAndCheckFakeSeed(BUFFER_SIZE / 2);
    assertEquals(FAKE_SEED_GENERATOR.countCalls(), 1);
    generateAndCheckFakeSeed(BUFFER_SIZE / 2);
    assertEquals(FAKE_SEED_GENERATOR.countCalls(), 2);
  }
}
