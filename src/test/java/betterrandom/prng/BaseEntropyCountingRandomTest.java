package betterrandom.prng;

import static betterrandom.prng.RandomTestUtils.DEFAULT_SEEDER;
import static betterrandom.prng.RandomTestUtils.assertMonteCarloPiEstimateSane;
import static org.testng.Assert.assertFalse;

import betterrandom.seed.SeedException;
import java.util.Arrays;
import org.testng.annotations.Test;

public abstract class BaseEntropyCountingRandomTest extends BaseRandomTest {

  @Override
  protected abstract BaseEntropyCountingRandom tryCreateRng() throws SeedException;

  @Override
  protected BaseEntropyCountingRandom createRng() {
    try {
      return tryCreateRng();
    } catch (SeedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test(timeOut = 15000)
  public void testReseeding() throws Exception {
    BaseEntropyCountingRandom rng = createRng();
    byte[] oldSeed = rng.getSeed();
    rng.setSeederThread(DEFAULT_SEEDER);
    rng.nextBytes(new byte[20000]);
    Thread.sleep(2000);
    byte[] newSeed = rng.getSeed();
    assertFalse(Arrays.equals(oldSeed, newSeed));
  }
}
