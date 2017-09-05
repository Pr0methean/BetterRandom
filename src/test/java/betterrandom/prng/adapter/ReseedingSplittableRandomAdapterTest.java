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
}
