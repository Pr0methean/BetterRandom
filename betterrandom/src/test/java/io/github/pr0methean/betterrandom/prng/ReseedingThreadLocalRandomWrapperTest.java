package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.FailingSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import java8.util.function.LongFunction;

public class ReseedingThreadLocalRandomWrapperTest extends ThreadLocalRandomWrapperTest {

  @Override public void testWrapLegacy() throws SeedException {
    ReseedingThreadLocalRandomWrapper
        .wrapLegacy(new RandomColonColonNewForLong(), DefaultSeedGenerator.DEFAULT_SEED_GENERATOR).nextInt();
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return super.getClassUnderTest();
  }

  @Override protected BaseRandom createRng() throws SeedException {
    return new ReseedingThreadLocalRandomWrapper(FailingSeedGenerator.FAILING_SEED_GENERATOR,
        new MersenneTwisterRandomColonColonNew());
  }

  private static class RandomColonColonNewForLong implements LongFunction<Random> {
    @Override public Random apply(long seed) {
      return new Random(seed);
    }
  }
}
