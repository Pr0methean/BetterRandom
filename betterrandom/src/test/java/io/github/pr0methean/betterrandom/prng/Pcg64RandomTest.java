package io.github.pr0methean.betterrandom.prng;

import static org.testng.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;
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

  // nextInt is excluded from assertion due to https://github.com/Pr0methean/BetterRandom/issues/13
  @Override public void testThreadSafety() {
    testThreadSafety(ImmutableList.of(NEXT_LONG, NEXT_GAUSSIAN, NEXT_DOUBLE));
    testThreadSafetyVsCrashesOnly(ImmutableList.of(NEXT_INT, NEXT_LONG));
    testThreadSafetyVsCrashesOnly(ImmutableList.of(NEXT_INT, NEXT_GAUSSIAN));
    testThreadSafetyVsCrashesOnly(ImmutableList.of(NEXT_INT, NEXT_DOUBLE));
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
