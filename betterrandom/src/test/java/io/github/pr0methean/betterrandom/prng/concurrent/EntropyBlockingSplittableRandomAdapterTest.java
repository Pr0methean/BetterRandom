package io.github.pr0methean.betterrandom.prng.concurrent;

import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.assertEquivalent;

import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Map;

public class EntropyBlockingSplittableRandomAdapterTest extends SplittableRandomAdapterTest {
  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return EntropyBlockingSplittableRandomAdapter.class;
  }

  @Override public void testRepeatability() throws SeedException {
    final BaseRandom rng = createRngLargeEntropyLimit();
    rng.setSeed(TEST_SEED);
    // Create second RNG using same seed.
    final BaseRandom duplicateRNG = createRngLargeEntropyLimit();
    duplicateRNG.setSeed(TEST_SEED);
    assertEquivalent(rng, duplicateRNG, 1000, "Generated sequences do not match");
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

  @Override protected RandomTestUtils.EntropyCheckMode getEntropyCheckMode() {
    return RandomTestUtils.EntropyCheckMode.LOWER_BOUND;
  }

  @Override public void testSetSeedLong() throws SeedException {
    final BaseRandom rng = createRngLargeEntropyLimit();
    final BaseRandom rng2 = createRngLargeEntropyLimit();
    rng.nextLong(); // ensure they won't both be in initial state before reseeding
    rng.setSeed(0x0123456789ABCDEFL);
    rng2.setSeed(0x0123456789ABCDEFL);
    assertEquivalent(rng, rng2, 20, "Output mismatch after reseeding with same seed");
  }

  private EntropyBlockingSplittableRandomAdapter createRngLargeEntropyLimit() {
    return new EntropyBlockingSplittableRandomAdapter(EntropyBlockingTestUtils.VERY_LOW_MINIMUM_ENTROPY, getTestSeedGenerator());
  }

  private EntropyBlockingSplittableRandomAdapter createRngLargeEntropyLimit(byte[] seed) {
    EntropyBlockingSplittableRandomAdapter out
        = new EntropyBlockingSplittableRandomAdapter(EntropyBlockingTestUtils.VERY_LOW_MINIMUM_ENTROPY, getTestSeedGenerator());
    out.setSeed(seed); // ensure seed is set for the current thread, not the master!
    return out;
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
