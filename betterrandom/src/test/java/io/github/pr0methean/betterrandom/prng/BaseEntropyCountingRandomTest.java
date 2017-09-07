package io.github.pr0methean.betterrandom.prng;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Arrays;
import org.testng.annotations.Test;

public abstract class BaseEntropyCountingRandomTest extends BaseRandomTest {

  @Override
  protected abstract BaseEntropyCountingRandom tryCreateRng() throws SeedException;

  @Override
  protected BaseEntropyCountingRandom createRng() {
    try {
      return tryCreateRng();
    } catch (final SeedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test(timeOut = 15000)
  public void testReseeding() throws Exception {
    final BaseEntropyCountingRandom rng = createRng();
    final byte[] oldSeed = rng.getSeed();
    rng.setSeederThread(RandomTestUtils.DEFAULT_SEEDER);
    rng.nextBytes(new byte[20000]);
    Thread.sleep(2000);
    final byte[] newSeed = rng.getSeed();
    assertFalse(Arrays.equals(oldSeed, newSeed));
  }

  @Override
  @Test(timeOut = 1000)
  public void testWithProbability() {
    BaseEntropyCountingRandom prng = createRng();
    long originalEntropy = prng.entropyBits();
    assertFalse(prng.withProbability(0.0));
    assertTrue(prng.withProbability(1.0));
    assertEquals(originalEntropy, prng.entropyBits());
    prng.withProbability(0.5);
    assertEquals(originalEntropy - 1, prng.entropyBits());
  }
}
