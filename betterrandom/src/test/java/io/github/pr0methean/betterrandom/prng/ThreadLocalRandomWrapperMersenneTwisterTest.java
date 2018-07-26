package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.SerializableSupplier;
import java.util.Map;
import java8.util.function.Function;
import java8.util.function.Supplier;
import org.testng.annotations.Test;

@Test(testName = "ThreadLocalRandomWrapper:MersenneTwisterRandom")
public class ThreadLocalRandomWrapperMersenneTwisterTest extends ThreadLocalRandomWrapperTest {

  private Supplier<? extends BaseRandom> mtSupplier;

  public ThreadLocalRandomWrapperMersenneTwisterTest() {
    // Must be done first, or else lambda won't be serializable.
    SeedGenerator seedGenerator = getTestSeedGenerator();

    mtSupplier = (Serializable & Supplier<BaseRandom>)
        () -> new MersenneTwisterRandom(seedGenerator);
  }

  @Override public Map<Class<?>, Object> constructorParams() {
    final Map<Class<?>, Object> params = super.constructorParams();
    params.put(Supplier.class, mtSupplier);
    params.put(Function.class, mtSupplier);
    return params;
  }

  @Override protected BaseRandom createRng() throws SeedException {
    return new ThreadLocalRandomWrapper(mtSupplier);
  }

  protected static class MersenneTwisterRandomColonColonNew
      implements SerializableSupplier<MersenneTwisterRandom>, Function<byte[], BaseRandom> {

    @Override public MersenneTwisterRandom get() {
      return new MersenneTwisterRandom();
    }

    @Override public BaseRandom apply(byte[] seed) {
      return new MersenneTwisterRandom(seed);
    }
  }
}
