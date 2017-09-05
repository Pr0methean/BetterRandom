package betterrandom.prng.adapter;

import betterrandom.DeadlockWatchdogThread;
import betterrandom.seed.SeedException;
import org.testng.annotations.BeforeClass;

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
