package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.SerializableSupplier;
import java.util.Map;
import java8.util.function.Function;
import java8.util.function.Supplier;

public class ThreadLocalRandomWrapperMersenneTwisterTest extends ThreadLocalRandomWrapperTest {

  @Override public Map<Class<?>, Object> constructorParams() {
    final Map<Class<?>, Object> params = super.constructorParams();
    params.put(Supplier.class, new MersenneTwisterRandomColonColonNew());
    params.put(Function.class, new MersenneTwisterRandomColonColonNew());
    return params;
  }

  @Override protected BaseRandom createRng() throws SeedException {
    return new ThreadLocalRandomWrapper(new MersenneTwisterRandomColonColonNew());
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
