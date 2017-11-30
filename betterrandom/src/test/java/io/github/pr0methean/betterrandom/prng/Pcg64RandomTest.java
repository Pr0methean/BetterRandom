package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedException;
import org.testng.annotations.Test;

public class Pcg64RandomTest extends BaseRandomTest {

  private static final int ITERATIONS = 8;

  @Test public void testAdvanceForward() {
    Pcg64Random copy1 = createRng();
    Pcg64Random copy2 = createRng(copy1.getSeed());
    for (int i = 0; i < ITERATIONS; i++) {
      copy1.nextInt();
    }
    copy2.advance(ITERATIONS);
    RandomTestUtils.testEquivalence(copy1, copy2, 20);
  }

  @Test public void testAdvanceBackward() {
    Pcg64Random copy1 = createRng();
    Pcg64Random copy2 = createRng(copy1.getSeed());
    for (int i = 0; i < ITERATIONS; i++) {
      copy1.nextInt();
    }
    copy1.advance(-ITERATIONS);
    RandomTestUtils.testEquivalence(copy1, copy2, 20);
  }

  @Test public void testAdvanceZero() {
    Pcg64Random copy1 = createRng();
    Pcg64Random copy2 = createRng(copy1.getSeed());
    copy2.advance(0);
    RandomTestUtils.testEquivalence(copy1, copy2, 20);
  }

  @Override public void testSetSeedLong() throws SeedException {
    final BaseRandom rng = createRng();
    final BaseRandom rng2 = createRng();
    rng.nextLong(); // ensure they won't both be in initial state before reseeding
    rng.setSeed(0x0123456789ABCDEFL);
    rng2.setSeed(0x0123456789ABCDEFL);
    assert RandomTestUtils.testEquivalence(rng, rng2, 20)
        : "Output mismatch after reseeding with same seed";
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
