package betterrandom.prng.adapter;

import static betterrandom.prng.RandomTestUtils.serializeAndDeserialize;
import static org.testng.Assert.assertEquals;

import betterrandom.DeadlockWatchdogThread;
import betterrandom.seed.SeedException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ReseedingSplittableRandomAdapterTest extends SingleThreadSplittableRandomAdapterTest {

  @BeforeClass
  public static void setUp() {
    DeadlockWatchdogThread.ensureStarted();
  }

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
  public void testDump() {
    // No-op.
  }
}
