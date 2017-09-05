package io.github.pr0methean.betterrandom.prng.adapter;

import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.serializeAndDeserialize;
import static io.github.pr0methean.betterrandom.seed.RandomDotOrgSeedGenerator.RANDOM_DOT_ORG_SEED_GENERATOR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

import io.github.pr0methean.betterrandom.DeadlockWatchdogThread;
import io.github.pr0methean.betterrandom.seed.DevRandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ReseedingSplittableRandomAdapterTest extends SingleThreadSplittableRandomAdapterTest {

  @Override
  protected ReseedingSplittableRandomAdapter tryCreateRng() throws SeedException {
    return ReseedingSplittableRandomAdapter.getDefaultInstance();
  }

  // FIXME: Why does this need more time than other PRNGs?!
  @Test(timeOut = 120_000)
  @Override
  public void testDistribution() throws SeedException {
    super.testDistribution();
  }

  // FIXME: Why does this need more time than other PRNGs?!
  @Test(timeOut = 120_000)
  @Override
  public void testStandardDeviation() throws SeedException {
    super.testStandardDeviation();
  }

  @Override
  public void testSerializable() throws SeedException {
    BaseSplittableRandomAdapter adapter = tryCreateRng();
    assertEquals(adapter, serializeAndDeserialize(adapter));
  }

  @Override
  public void testSetSeed() {
    // No-op.
  }

  @Override
  @Test
  public void testSeedTooShort() {
    // No-op.
  }

  @Override
  @Test
  public void testSeedTooLong() {
    // No-op.
  }

  @Override
  public void testDump() throws SeedException {
    assertNotEquals(
        ReseedingSplittableRandomAdapter.getInstance(RANDOM_DOT_ORG_SEED_GENERATOR).dump(),
        ReseedingSplittableRandomAdapter.getInstance(
            SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR).dump());
  }

  @Test
  public void testFinalize() throws SeedException {
    ReseedingSplittableRandomAdapter.getInstance(DevRandomSeedGenerator.DEV_RANDOM_SEED_GENERATOR);
    Runtime.getRuntime().runFinalization();
  }
}
