package io.github.pr0methean.betterrandom.prng;

import static org.testng.Assert.assertEquals;

import java.util.Random;
import java.util.concurrent.ConcurrentSkipListSet;
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

  // Has a disabled assertion due to https://github.com/Pr0methean/BetterRandom/issues/13
  @Override public void testThreadSafety() {
    for (int i=0; i<5; i++) {
      // This loop is necessary to control the false pass rate, especially during mutation testing.
      ConcurrentSkipListSet<Long> sequentialLongs = new ConcurrentSkipListSet<>();
      ConcurrentSkipListSet<Long> parallelLongs = new ConcurrentSkipListSet<>();
      runSequentialAndParallel(sequentialLongs, parallelLongs, Random::nextLong);
      assertEquals(parallelLongs, sequentialLongs);
      ConcurrentSkipListSet<Double> sequentialDoubles = new ConcurrentSkipListSet<>();
      ConcurrentSkipListSet<Double> parallelDoubles = new ConcurrentSkipListSet<>();
      runSequentialAndParallel(sequentialDoubles, parallelDoubles, Random::nextDouble);
      assertEquals(parallelDoubles, sequentialDoubles);
      sequentialDoubles.clear();
      parallelDoubles.clear();
      runSequentialAndParallel(sequentialDoubles, parallelDoubles, Random::nextGaussian);
      assertEquals(parallelDoubles, sequentialDoubles);
      ConcurrentSkipListSet<Integer> sequentialInts = new ConcurrentSkipListSet<>();
      ConcurrentSkipListSet<Integer> parallelInts = new ConcurrentSkipListSet<>();
      runSequentialAndParallel(sequentialInts, parallelInts, Random::nextInt);
      // assertEquals(parallelInts, sequentialInts);
    }
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
