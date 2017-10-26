package io.github.pr0methean.betterrandom.prng;

import static org.testng.Assert.assertFalse;

import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import java.util.Arrays;
import org.testng.annotations.Test;

public class Pcg64RandomTest extends BaseRandomTest {

  private static final int ITERATIONS = 8;

  @Test
  public void testAdvanceForward() {
    Pcg64Random copy1 = createRng();
    Pcg64Random copy2 = createRng(copy1.getSeed());
    for (int i=0; i < ITERATIONS; i++) {
      copy1.nextInt();
    }
    copy2.advance(ITERATIONS);
    RandomTestUtils.testEquivalence(copy1, copy2, 20);
  }

  @Override public void testReseeding() throws Exception {
    final BaseRandom rng = createRng();
    rng.setSeederThread(
        RandomSeederThread.getInstance(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR));
    final byte[] oldSeed = rng.getSeed();
    rng.nextBytes(new byte[oldSeed.length + 1]);
    Thread.sleep(5000);
    final byte[] newSeed = rng.getSeed();
    assertFalse(Arrays.equals(oldSeed, newSeed));
    rng.setSeederThread(null);
  }

  @Test
  public void testAdvanceBackward() {
    Pcg64Random copy1 = createRng();
    Pcg64Random copy2 = createRng(copy1.getSeed());
    for (int i=0; i < ITERATIONS; i++) {
      copy1.nextInt();
    }
    copy1.advance(-ITERATIONS);
    RandomTestUtils.testEquivalence(copy1, copy2, 20);
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return Pcg64Random.class;
  }

  @Override protected Pcg64Random createRng() {
    return new Pcg64Random();
  }

  @Override protected Pcg64Random createRng(byte[] seed) {
    return new Pcg64Random(seed);
  }
}
