package io.github.pr0methean.betterrandom.seed;

import static io.github.pr0methean.betterrandom.TestUtils.fail;
import static io.github.pr0methean.betterrandom.seed.FailingSeedGenerator.DEFAULT_INSTANCE;
import static java.util.Collections.singletonList;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import org.testng.annotations.Test;

public class SeedGeneratorPreferenceListTest extends SeedGeneratorTest<SeedGeneratorPreferenceList> {

  @Override public void testToString() {
    seedGenerator = new SeedGeneratorPreferenceList(singletonList(new FakeSeedGenerator()), true);
    super.testToString();
  }

  @Test public void testFirstSucceeds() {
    FailingSeedGenerator shouldNotBeUsed
        = new FailingSeedGenerator("Should not have fallen through to this SeedGenerator");
    seedGenerator =
        new SeedGeneratorPreferenceList(Arrays.asList(new FakeSeedGenerator(), shouldNotBeUsed),
            true);
    generateAndCheckFakeSeed(32);
  }

  @Test public void testSecondSucceeds() {
    seedGenerator = new SeedGeneratorPreferenceList(
        Arrays.asList(DEFAULT_INSTANCE, new FakeSeedGenerator()), true);
    generateAndCheckFakeSeed(32);
  }

  @Test public void testAlwaysWorthTrying() {
    FakeSeedGenerator doNotCall = new FakeSeedGenerator() {
      private static final long serialVersionUID = 7594951403926684340L;

      @Override public boolean isWorthTrying() {
        throw fail("isWorthTrying() should not have been called");
      }
    };
    seedGenerator = new SeedGeneratorPreferenceList(singletonList(doNotCall), true);
    assertTrue(seedGenerator.isWorthTrying());
  }

  @Test public void testNotAlwaysWorthTrying() {
    assertFalse(seedGenerator.isWorthTrying());
  }

  @Override protected SeedGeneratorPreferenceList initializeSeedGenerator() {
    return new SeedGeneratorPreferenceList(singletonList(DEFAULT_INSTANCE), false);
  }
}
