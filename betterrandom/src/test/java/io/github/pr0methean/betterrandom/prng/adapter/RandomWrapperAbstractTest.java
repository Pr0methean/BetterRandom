package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.NamedFunction;
import io.github.pr0methean.betterrandom.prng.BaseRandomTest;
import java.util.Random;

public abstract class RandomWrapperAbstractTest<T extends RandomWrapper<TWrapped>,
    TWrapped extends Random>
    extends BaseRandomTest<T> {

  protected abstract TWrapped createWrappedPrng();

  protected abstract TWrapped createWrappedPrng(byte[] seed);

  protected final NamedFunction<T, Double> setWrapped
      = new NamedFunction<>(random -> {
    random.setWrapped(createWrappedPrng());
    return 0.0;
  }, "setWrapped");
}
