package io.github.pr0methean.betterrandom.seed;

import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.Test;

public abstract class AbstractSeedGeneratorTest extends PowerMockTestCase {

  protected SeedGenerator seedGenerator;

  protected AbstractSeedGeneratorTest(final SeedGenerator seedGenerator) {
    this.seedGenerator = seedGenerator;
  }

  @Test public void testToString() {
    Assert.assertNotNull(seedGenerator.toString());
  }
}
