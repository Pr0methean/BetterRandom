package io.github.pr0methean.betterrandom.prng;

import com.google.common.collect.ImmutableMap;
import io.github.pr0methean.betterrandom.TestUtils;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;

public class RandomWrapperAesCounterRandomTest extends AesCounterRandom128Test {

  @Override
  protected Class<? extends BaseRandom> getClassUnderTest() {
    return RandomWrapper.class;
  }

  @Override
  public void testAllPublicConstructors()
      throws SeedException, IllegalAccessException, InstantiationException, InvocationTargetException {
    BaseRandom basePrng = createRng();
    int seedLength = getNewSeedLength(basePrng);
    TestUtils.testAllPublicConstructors(getClassUnderTest(), ImmutableMap.of(
        int.class, seedLength,
        byte[].class, DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(seedLength),
        SeedGenerator.class, DefaultSeedGenerator.DEFAULT_SEED_GENERATOR,
        Random.class, new AesCounterRandom()
    ), BaseRandom::nextInt);
  }

  @Override
  protected RandomWrapper tryCreateRng() throws SeedException {
    return new RandomWrapper(new AesCounterRandom());
  }

  @Override
  protected RandomWrapper createRng(final byte[] seed) throws SeedException {
    return new RandomWrapper(new AesCounterRandom(seed));
  }
}
