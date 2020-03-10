package io.github.pr0methean.betterrandom.prng;

public class Pcg128RandomTest extends SeekableRandomTest<Pcg128Random> {
  @Override protected Class<? extends Pcg128Random> getClassUnderTest() {
    return Pcg128Random.class;
  }

  @Override protected Pcg128Random createRng() {
    return new Pcg128Random(getTestSeedGenerator());
  }

  @Override protected Pcg128Random createRng(final byte[] seed) {
    return new Pcg128Random(seed);
  }
}