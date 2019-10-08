package io.github.pr0methean.betterrandom.prng.concurrent;

import com.google.common.collect.ImmutableMap;
import io.github.pr0methean.betterrandom.TestUtils;
import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import org.testng.annotations.Test;

@Test(testName = "EntropyBlockingRandomWrapper:AesCounterRandom")
public class EntropyBlockingRandomWrapperAesCounterRandomTest extends RandomWrapperAesCounterRandomTest {
  private static final long DEFAULT_MAX_ENTROPY = -1000L;

  @Override @Test public void testAllPublicConstructors() {
    Constructor<?>[] constructors =
        EntropyBlockingRandomWrapper.class.getDeclaredConstructors();
    ArrayList<Constructor<?>> relevantConstructors = new ArrayList<>(constructors.length);
    for (Constructor<?> constructor : constructors) {
      if (Arrays.asList(constructor.getParameterTypes()).contains(Random.class)) {
        relevantConstructors.add(constructor);
      }
    }
    TestUtils.testConstructors(false, ImmutableMap.copyOf(constructorParams()),
        (Consumer<? super EntropyBlockingRandomWrapper>) BaseRandom::nextInt,
        relevantConstructors);
  }

  @Override public Class<? extends BaseRandom> getClassUnderTest() {
    return EntropyBlockingRandomWrapper.class;
  }

  @Override protected RandomWrapper createRng() throws SeedException {
    SeedGenerator testSeedGenerator = getTestSeedGenerator();
    return new EntropyBlockingRandomWrapper(new AesCounterRandom(testSeedGenerator), DEFAULT_MAX_ENTROPY,
        testSeedGenerator);
  }

  @Override public Map<Class<?>, Object> constructorParams() {
    Map<Class<?>, Object> out = super.constructorParams();
    out.put(long.class, DEFAULT_MAX_ENTROPY);
    out.put(Random.class, new AesCounterRandom(getTestSeedGenerator()));
    return out;
  }

  @Override public void testRepeatability() throws SeedException {
    super.testRepeatability();
  }

  // FIXME: Too slow!
  @Override @Test(timeOut = 120_000L) public void testRandomSeederThreadIntegration() {
    super.testRandomSeederThreadIntegration();
  }
}
