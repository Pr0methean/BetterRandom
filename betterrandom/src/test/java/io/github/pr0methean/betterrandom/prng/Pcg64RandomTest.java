package io.github.pr0methean.betterrandom.prng;

import org.testng.annotations.Test;

@Test(testName = "Pcg64Random")
public class Pcg64RandomTest extends SeekableRandomTest {

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
