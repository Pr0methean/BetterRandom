package io.github.pr0methean.betterrandom.seed;

import org.testng.Assert;
import org.testng.annotations.Test;

public abstract class AbstractSeedGeneratorTest {
  protected final SeedGenerator seedGenerator;

  public AbstractSeedGeneratorTest(SeedGenerator seedGenerator) {
    this.seedGenerator = seedGenerator;
  }

  @Test
  public void testToString() {
    Assert.assertNotNull(seedGenerator.toString());
  }
}
