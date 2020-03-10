package io.github.pr0methean.betterrandom.prng.adapter;

import static org.testng.Assert.assertSame;

import com.google.common.collect.ImmutableList;
import io.github.pr0methean.betterrandom.NamedFunction;
import io.github.pr0methean.betterrandom.prng.MersenneTwisterRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.util.Collections;
import java.util.Random;
import org.testng.annotations.Test;

@Test(testName = "RandomWrapper:MersenneTwisterRandom")
public class RandomWrapperMersenneTwisterRandomTest
    extends RandomWrapperAbstractTest<RandomWrapper<MersenneTwisterRandom>> {

  private final NamedFunction<Random, Double> setWrapped;

  public RandomWrapperMersenneTwisterRandomTest() {
    final SeedGenerator seedGenerator = getTestSeedGenerator();
    setWrapped = new NamedFunction<>(random -> {
      ((RandomWrapper<Random>) random).setWrapped(new MersenneTwisterRandom(seedGenerator));
      return 0.0;
    }, "setWrapped");
  }

  /**
   * Assertion-free with respect to the long/double methods because, contrary to its contract to be
   * thread-safe, {@link Random#nextLong()} is not transactional. Rather, it uses two subroutine
   * calls that can interleave with calls from other threads.
   */
  @Override public void testThreadSafety() {
    checkThreadSafety(ImmutableList.of(NEXT_INT), Collections.emptyList());
    testThreadSafetyVsCrashesOnly(30,
        ImmutableList.of(NEXT_LONG, NEXT_INT, NEXT_DOUBLE, NEXT_GAUSSIAN, setWrapped));
  }

  @SuppressWarnings("rawtypes")
  @Override protected Class<RandomWrapper> getClassUnderTest() {
    return RandomWrapper.class;
  }

  @Override @Test(enabled = false) public void testAllPublicConstructors() throws SeedException {
    // No-op: redundant to super insofar as it works.
  }

  @Override protected RandomWrapper<MersenneTwisterRandom> createRng() throws SeedException {
    return new RandomWrapper<>(new MersenneTwisterRandom(getTestSeedGenerator()));
  }

  @Override protected RandomWrapper<MersenneTwisterRandom> createRng(final byte[] seed) throws SeedException {
    return new RandomWrapper<>(new MersenneTwisterRandom(seed));
  }

  // FIXME: This test takes too long!
  @Override @Test(timeOut = 120_000) public void testSerializable() throws SeedException {
    super.testSerializable();
  }

  @Override @Test public void testReseeding()
      throws SeedException {
    super.testReseeding();
  }

  @Test public void testGetWrapped() {
    assertSame(createRng().getWrapped().getClass(), MersenneTwisterRandom.class);
  }
}
