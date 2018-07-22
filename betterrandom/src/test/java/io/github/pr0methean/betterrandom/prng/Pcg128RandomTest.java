package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.DeadlockWatchdogThread;
import org.testng.annotations.BeforeClass;

public class Pcg128RandomTest extends SeekableRandomTest {
  @BeforeClass
  public void setUpClass() {
    DeadlockWatchdogThread.ensureStarted();
  }

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