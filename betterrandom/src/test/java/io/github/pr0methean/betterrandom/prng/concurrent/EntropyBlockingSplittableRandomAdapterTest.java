package io.github.pr0methean.betterrandom.prng.concurrent;

import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Map;

public class EntropyBlockingSplittableRandomAdapterTest extends SplittableRandomAdapterTest {
  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return EntropyBlockingSplittableRandomAdapter.class;
  }

  @Override public Map<Class<?>, Object> constructorParams() {
    Map<Class<?>, Object> out = super.constructorParams();
    out.put(long.class, EntropyBlockingTestUtils.DEFAULT_MAX_ENTROPY);
    return out;
  }

  @Override protected SplittableRandomAdapter createRng() throws SeedException {
    return new EntropyBlockingSplittableRandomAdapter(EntropyBlockingTestUtils.DEFAULT_MAX_ENTROPY,
        getTestSeedGenerator());
  }

  @Override protected BaseRandom createRng(byte[] seed) throws SeedException {
    return new EntropyBlockingSplittableRandomAdapter(seed,
        EntropyBlockingTestUtils.DEFAULT_MAX_ENTROPY, getTestSeedGenerator());
  }

  private EntropyBlockingSplittableRandomAdapter createRngLargeEntropyLimit() {
    return new EntropyBlockingSplittableRandomAdapter(EntropyBlockingTestUtils.VERY_LOW_MINIMUM_ENTROPY, getTestSeedGenerator());
  }

  private EntropyBlockingSplittableRandomAdapter createRngLargeEntropyLimit(byte[] seed) {
    return new EntropyBlockingSplittableRandomAdapter(seed, EntropyBlockingTestUtils.VERY_LOW_MINIMUM_ENTROPY, getTestSeedGenerator());
  }

  @Override public void testSetSeedAfterNextLong() throws SeedException {
    checkSetSeedAfter(this::createRngLargeEntropyLimit, this::createRngLargeEntropyLimit,
        BaseRandom::nextLong);
  }

  @Override public void testSetSeedAfterNextInt() throws SeedException {
    checkSetSeedAfter(this::createRngLargeEntropyLimit, this::createRngLargeEntropyLimit,
        BaseRandom::nextInt);
  }
}
