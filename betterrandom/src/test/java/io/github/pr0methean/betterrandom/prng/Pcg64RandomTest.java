package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedException;
import org.testng.annotations.Test;

@Test(testName = "Pcg64Random") public class Pcg64RandomTest extends SeekableRandomTest<Pcg64Random> {

  @Override public void testSetSeedLong() throws SeedException {
    final Pcg64Random rng = createRng();
    final Pcg64Random rng2 = createRng();
    BaseRandomTest.checkSetSeedLong(rng, rng2);
  }

  @Override protected Class<? extends Pcg64Random> getClassUnderTest() {
    return Pcg64Random.class;
  }

  @Override protected Pcg64Random createRng() {
    return new Pcg64Random(getTestSeedGenerator());
  }

  @Override protected Pcg64Random createRng(final byte[] seed) {
    return new Pcg64Random(seed);
  }
}
