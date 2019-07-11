package io.github.pr0methean.betterrandom.seed;

import static io.github.pr0methean.betterrandom.seed.FailingSeedGenerator.FAILING_SEED_GENERATOR;
import static java.util.Collections.singletonList;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import org.testng.annotations.Test;

public class SeedGeneratorPreferenceListTest extends AbstractSeedGeneratorTest {
  public SeedGeneratorPreferenceListTest() {
    super(null);
  }

  @Override public void testToString() {
    seedGenerator = new SeedGeneratorPreferenceList(singletonList(new FakeSeedGenerator()), true);
    super.testToString();
  }

  @Test public void testFirstSucceeds() {
    FakeSeedGenerator shouldNotBeUsed = new FakeSeedGenerator() {
      @Override public void generateSeed(byte[] output) throws SeedException {
        throw new AssertionError("Should not have fallen through to this SeedGenerator");
      }
    };
    seedGenerator =
        new SeedGeneratorPreferenceList(Arrays.asList(new FakeSeedGenerator(), shouldNotBeUsed),
            true);
    generateAndCheckFakeSeed(32);
  }

  @Test public void testSecondSucceeds() {
    seedGenerator = new SeedGeneratorPreferenceList(
        Arrays.asList(FAILING_SEED_GENERATOR, new FakeSeedGenerator()), true);
    generateAndCheckFakeSeed(32);
  }

  @Test public void testAlwaysWorthTrying() {
    FakeSeedGenerator doNotCall = new FakeSeedGenerator() {
      @Override public boolean isWorthTrying() {
        throw new AssertionError("isWorthTrying() should not have been called");
      }
    };
    seedGenerator = new SeedGeneratorPreferenceList(singletonList(doNotCall), true);
    assertTrue(seedGenerator.isWorthTrying());
  }

  @Test public void testNotAlwaysWorthTrying() {
    seedGenerator = new SeedGeneratorPreferenceList(singletonList(FAILING_SEED_GENERATOR), false);
    assertFalse(seedGenerator.isWorthTrying());
  }
}
