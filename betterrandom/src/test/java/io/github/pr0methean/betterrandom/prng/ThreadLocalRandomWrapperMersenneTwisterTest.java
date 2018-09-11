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

  private final MersenneTwisterRandomColonColonNew mtSupplier
      = new MersenneTwisterRandomColonColonNew(getTestSeedGenerator());

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

    private final SeedGenerator seedGenerator;

    public MersenneTwisterRandomColonColonNew(SeedGenerator seedGenerator) {
      this.seedGenerator = seedGenerator;
    }

    @Override public MersenneTwisterRandom get() {
      return new MersenneTwisterRandom(seedGenerator);
    }

    @Override public BaseRandom apply(byte[] seed) {
      return new MersenneTwisterRandom(seed);
    }
  }
}
