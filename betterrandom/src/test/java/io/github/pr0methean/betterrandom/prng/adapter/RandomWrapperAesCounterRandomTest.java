package io.github.pr0methean.betterrandom.prng.adapter;

import static org.testng.Assert.assertSame;

import com.google.common.collect.ImmutableList;
import io.github.pr0methean.betterrandom.NamedFunction;
import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.prng.AesCounterRandomTest;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import org.testng.annotations.Test;

@Test(testName = "RandomWrapper:AesCounterRandom") public class RandomWrapperAesCounterRandomTest
    extends AesCounterRandomTest {

  private final NamedFunction<Random, Double> setWrapped = new NamedFunction<>(random -> {
    ((RandomWrapper<Random>) random).setWrapped(new AesCounterRandom(getTestSeedGenerator()));
    return 0.0;
  }, "setWrapped");

  public RandomWrapperAesCounterRandomTest() {
    seedSizeBytes = 16;
  }

  @Override @Test public void testThreadSafetySetSeed() {
    testThreadSafetyVsCrashesOnly(30,
        ImmutableList.of(NEXT_LONG, NEXT_INT, NEXT_DOUBLE, NEXT_GAUSSIAN, setSeed, setWrapped));
  }

  @Override @Test(enabled = false) public void testAdvanceForward(int delta) {
    // No-op: RandomWrapper isn't seekable
  }

  @Override @Test(enabled = false) public void testAdvanceBackward(int delta) {
    // No-op: RandomWrapper isn't seekable
  }

  @Override @Test(enabled = false) public void testAdvanceZero() {
    // No-op: RandomWrapper isn't seekable
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return RandomWrapper.class;
  }

  @Override @Test(enabled = false) public void testAllPublicConstructors() {
    // No-op: redundant to super insofar as it works.
  }

  @Override protected RandomWrapper<AesCounterRandom> createRng() throws SeedException {
    return new RandomWrapper<>(new AesCounterRandom(getTestSeedGenerator()));
  }

  @Override protected RandomWrapper<AesCounterRandom> createRng(final byte[] seed) throws SeedException {
    return new RandomWrapper<>(new AesCounterRandom(seed));
  }

  @Test public void testGetWrapped() {
    assertSame(createRng().getWrapped().getClass(), AesCounterRandom.class);
  }
}
