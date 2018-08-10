package io.github.pr0methean.betterrandom.prng;

import com.google.common.collect.ImmutableList;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;
import org.testng.annotations.Test;

@Test(testName = "RandomWrapper:MersenneTwisterRandom")
public class RandomWrapperMersenneTwisterRandomTest extends MersenneTwisterRandomTest {

  private final NamedFunction<Random, Double> setWrapped;

  public RandomWrapperMersenneTwisterRandomTest() {
    SeedGenerator seedGenerator = getTestSeedGenerator();
    setWrapped = new NamedFunction<>(random -> {
      ((RandomWrapper) random).setWrapped(new MersenneTwisterRandom(seedGenerator));
      return 0.0;
    }, "setWrapped");
  }

  @Override public void testThreadSafety() {
    super.testThreadSafety();
    testThreadSafetyVsCrashesOnly(30,
        ImmutableList.of(NEXT_LONG, NEXT_INT, NEXT_DOUBLE, NEXT_GAUSSIAN, setWrapped));
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return RandomWrapper.class;
  }

  @Override @Test(enabled = false) public void testAllPublicConstructors()
      throws SeedException, IllegalAccessException, InstantiationException,
      InvocationTargetException {
    // No-op: redundant to super insofar as it works.
  }

  @Override protected RandomWrapper createRng() throws SeedException {
    return new RandomWrapper(new MersenneTwisterRandom(getTestSeedGenerator()));
  }

  @Override protected RandomWrapper createRng(final byte[] seed) throws SeedException {
    return new RandomWrapper(new MersenneTwisterRandom(seed));
  }
}
