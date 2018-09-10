package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.io.Serializable;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.testng.annotations.Test;

@Test(testName = "ThreadLocalRandomWrapper:MersenneTwisterRandom")
public class ThreadLocalRandomWrapperMersenneTwisterTest extends ThreadLocalRandomWrapperTest {

  private Supplier<? extends BaseRandom> mtSupplier;

  public ThreadLocalRandomWrapperMersenneTwisterTest() {
    // Must be done first, or else lambda won't be serializable.
    final SeedGenerator seedGenerator = getTestSeedGenerator();

    mtSupplier = (Serializable & Supplier<BaseRandom>)
        () -> new MersenneTwisterRandom(seedGenerator);
  }

  @Override public Map<Class<?>, Object> constructorParams() {
    final Map<Class<?>, Object> params = super.constructorParams();
    params.put(Supplier.class, mtSupplier);
    params
        .put(Function.class, (Function<byte[], BaseRandom>) MersenneTwisterRandom::new);
    return params;
  }

  @Override protected BaseRandom createRng() throws SeedException {
    return new ThreadLocalRandomWrapper(mtSupplier);
  }
}
