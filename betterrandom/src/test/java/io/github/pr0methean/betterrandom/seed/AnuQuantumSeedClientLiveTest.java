package io.github.pr0methean.betterrandom.seed;

import org.testng.annotations.Test;

public class AnuQuantumSeedClientLiveTest extends WebSeedClientLiveTest<AnuQuantumSeedClient> {
  public AnuQuantumSeedClientLiveTest() {
    super(AnuQuantumSeedClient.WITHOUT_DELAYED_RETRY);
  }

  @Test
  public void testGeneratorSmallSeed() {
    SeedTestUtils.testGenerator(seedGenerator, true, 16);
  }

  @Test
  public void testGeneratorLargeRoundSize() {
    SeedTestUtils.testGenerator(seedGenerator, true, 2048);
  }

  @Test
  public void testGeneratorLargeOddSize() {
    SeedTestUtils.testGenerator(seedGenerator, true, 1337);
  }
}
