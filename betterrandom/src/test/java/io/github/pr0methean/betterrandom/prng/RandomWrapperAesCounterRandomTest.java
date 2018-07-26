package io.github.pr0methean.betterrandom.prng;

import com.google.common.collect.ImmutableList;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import org.testng.annotations.Test;

@Test(testName = "RandomWrapper:AesCounterRandom")
public class RandomWrapperAesCounterRandomTest extends AesCounterRandomTest {

  private static final NamedFunction<Random, Double> SET_WRAPPED = new NamedFunction<>(random -> {
    ((RandomWrapper) random).setWrapped(new AesCounterRandom());
    return 0.0;
  }, "setWrapped");

  public RandomWrapperAesCounterRandomTest() {
    super(16);
  }

  @Override @Test public void testThreadSafetySetSeed() {
    testThreadSafetyVsCrashesOnly(30,
        ImmutableList.of(NEXT_LONG, NEXT_INT, NEXT_DOUBLE, NEXT_GAUSSIAN, SET_SEED, SET_WRAPPED));
  }

  @Override @Test(enabled = false) public void testAdvanceForward() {
    // No-op: RandomWrapper isn't seekable
  }

  @Override @Test(enabled = false) public void testAdvanceBackward() {
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

  @Override protected RandomWrapper createRng() throws SeedException {
    return new RandomWrapper(new AesCounterRandom(getTestSeedGenerator()));
  }

  @Override protected RandomWrapper createRng(final byte[] seed) throws SeedException {
    return new RandomWrapper(new AesCounterRandom(seed));
  }
}
