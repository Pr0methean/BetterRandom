package io.github.pr0methean.betterrandom.seed;

import static io.github.pr0methean.betterrandom.seed.AnuQuantumSeedGenerator.ANU_QUANTUM_SEED_GENERATOR;

import org.testng.annotations.Test;

public class AnuQuantumSeedGeneratorLiveTest extends AbstractSeedGeneratorTest {
  protected AnuQuantumSeedGeneratorLiveTest() {
    super(ANU_QUANTUM_SEED_GENERATOR);
  }

  @Test
  public void testGeneratorSmallSeed() {
    SeedTestUtils.testGenerator(ANU_QUANTUM_SEED_GENERATOR, true, 16);
  }

  @Test
  public void testGeneratorLargeRoundSize() {
    SeedTestUtils.testGenerator(ANU_QUANTUM_SEED_GENERATOR, true, 2048);
  }

  @Test
  public void testGeneratorLargeOddSize() {
    SeedTestUtils.testGenerator(ANU_QUANTUM_SEED_GENERATOR, true, 1337);
  }
}
