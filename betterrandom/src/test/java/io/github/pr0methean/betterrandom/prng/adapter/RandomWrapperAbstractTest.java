package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.NamedFunction;
import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.prng.BaseRandomTest;
import java.util.Random;

public abstract class RandomWrapperAbstractTest<T extends RandomWrapper<? extends Random>>
    extends BaseRandomTest<T> {
  protected final NamedFunction<RandomWrapper<? super AesCounterRandom>, Double> setWrapped
      = new NamedFunction<>(random -> {
    random.setWrapped(new AesCounterRandom(getTestSeedGenerator()));
    return 0.0;
  }, "setWrapped");
}
