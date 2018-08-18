package io.github.pr0methean.betterrandom.prng;

public class Pcg128RandomTest extends SeekableRandomTest {
  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return Pcg128Random.class;
  }

  @Override protected BaseRandom createRng() {
    return new Pcg128Random();
  }

  @Override protected BaseRandom createRng(byte[] seed) {
    return new Pcg128Random(seed);
  }
}