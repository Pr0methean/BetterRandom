package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedException;
import org.testng.annotations.Test;

@Test(testName = "Pcg64Random")
public class Pcg64RandomTest extends SeekableRandomTest {

  @Override public void testSetSeedLong() throws SeedException {
    final BaseRandom rng = createRng();
    final BaseRandom rng2 = createRng();
    rng.nextLong(); // ensure they won't both be in initial state before reseeding
    rng.setSeed(0x0123456789ABCDEFL);
    rng2.setSeed(0x0123456789ABCDEFL);
    RandomTestUtils.assertEquivalent(rng, rng2, 20,
        "Output mismatch after reseeding with same seed");
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return Pcg64Random.class;
  }

  @Override protected Pcg64Random createRng() {
    return new Pcg64Random(getTestSeedGenerator());
  }

  @Override protected Pcg64Random createRng(final byte[] seed) {
    return new Pcg64Random(seed);
  }
}
