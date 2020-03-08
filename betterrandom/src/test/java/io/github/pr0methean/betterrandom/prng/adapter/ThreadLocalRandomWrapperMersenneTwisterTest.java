package io.github.pr0methean.betterrandom.prng.adapter;

import static org.testng.Assert.assertSame;

import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.MersenneTwisterRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.io.Serializable;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.testng.annotations.Test;

public class ThreadLocalRandomWrapperMersenneTwisterTest extends ThreadLocalRandomWrapperTest {

  private final Supplier<MersenneTwisterRandom> mtSupplier;

  public ThreadLocalRandomWrapperMersenneTwisterTest() {
    // Must be done first, or else lambda won't be serializable.
    final SeedGenerator seedGenerator = getTestSeedGenerator();

    mtSupplier =
        (Serializable & Supplier<MersenneTwisterRandom>) () -> new MersenneTwisterRandom(seedGenerator);
  }

  @Override protected Map<Class<?>, Object> constructorParams() {
    final Map<Class<?>, Object> params = super.constructorParams();
    params.put(Supplier.class, mtSupplier);
    params.put(Function.class, (Function<byte[], BaseRandom>) MersenneTwisterRandom::new);
    return params;
  }

  @Override protected ThreadLocalRandomWrapper<MersenneTwisterRandom> createRng() throws SeedException {
    return new ThreadLocalRandomWrapper<>(mtSupplier);
  }

  @Override @Test public void testGetWrapped() {
    assertSame(createRng().getWrapped().getClass(), MersenneTwisterRandom.class);
  }
}
