package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.FailingSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.io.Serializable;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

public class ReseedingThreadLocalRandomWrapperTest extends ThreadLocalRandomWrapperTest {

  @Override public void testWrapLegacy() throws SeedException {
    ReseedingThreadLocalRandomWrapper
        .wrapLegacy(Random::new, FailingSeedGenerator.FAILING_SEED_GENERATOR).nextInt();
  }

  @Override public Map<Class<?>, Object> constructorParams() {
    // testNextLong may fail spuriously if a real seed generator replenishes the entropy too fast
    Map<Class<?>, Object> params = super.constructorParams();
    params.put(SeedGenerator.class, FailingSeedGenerator.FAILING_SEED_GENERATOR);
    return params;
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return super.getClassUnderTest();
  }

  @Override protected BaseRandom createRng() throws SeedException {
    return new ReseedingThreadLocalRandomWrapper(FailingSeedGenerator.FAILING_SEED_GENERATOR,
        (Serializable & Supplier<BaseRandom>) MersenneTwisterRandom::new);
  }
}
