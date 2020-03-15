package io.github.pr0methean.betterrandom.prng.adapter;

import static org.testng.Assert.assertSame;

import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.MersenneTwisterRandom;
import io.github.pr0methean.betterrandom.seed.PseudorandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.SerializableFunction;
import io.github.pr0methean.betterrandom.util.SerializableSupplier;
import java.util.Map;
import org.testng.annotations.Test;

public class ThreadLocalRandomWrapperMersenneTwisterTest extends
    ThreadLocalRandomWrapperTest<MersenneTwisterRandom> {

  public ThreadLocalRandomWrapperMersenneTwisterTest() {
    // Must be done first, or else lambda won't be serializable.
    super(createSupplier());
  }

  private static SerializableSupplier<MersenneTwisterRandom> createSupplier() {
    final SeedGenerator seedGenerator = new PseudorandomSeedGenerator();
    return () -> new MersenneTwisterRandom(seedGenerator);
  }

  @Override protected Map<Class<?>, Object> constructorParams() {
    final Map<Class<?>, Object> params = super.constructorParams();
    params.put(SerializableFunction.class, (SerializableFunction<byte[], BaseRandom>) MersenneTwisterRandom::new);
    return params;
  }

  @Override protected ThreadLocalRandomWrapper<MersenneTwisterRandom> createRng() throws SeedException {
    return new ThreadLocalRandomWrapper<>(supplier);
  }

  @Override @Test public void testGetWrapped() {
    assertSame(createRng().getWrapped().getClass(), MersenneTwisterRandom.class);
  }
}
