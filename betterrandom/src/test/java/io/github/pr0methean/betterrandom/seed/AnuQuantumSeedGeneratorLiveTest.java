package io.github.pr0methean.betterrandom.seed;

import static io.github.pr0methean.betterrandom.seed.AnuQuantumSeedGenerator.ANU_QUANTUM_SEED_GENERATOR;

import org.testng.annotations.Test;

public class AnuQuantumSeedGeneratorLiveTest extends WebJsonSeedGeneratorLiveTest<AnuQuantumSeedGenerator> {
  protected AnuQuantumSeedGeneratorLiveTest() {
    super(ANU_QUANTUM_SEED_GENERATOR);
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
