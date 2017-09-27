package io.github.pr0methean.betterrandom.prng.adapter;

import static io.github.pr0methean.betterrandom.prng.BaseRandom.ENTROPY_OF_DOUBLE;
import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.checkRangeAndEntropy;
import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.testEquivalence;

import io.github.pr0methean.betterrandom.TestingDeficiency;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.BaseRandomTest;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.CloneViaSerialization;
import org.testng.annotations.Test;

public class SingleThreadSplittableRandomAdapterTest extends BaseRandomTest {

  /**
   * Overridden in subclasses, so that subclassing the test can test the subclasses.
   */
  protected BaseSplittableRandomAdapter tryCreateRng() throws SeedException {
    return new SingleThreadSplittableRandomAdapter(
        DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  }

  @Override
  protected BaseRandom createRng(final byte[] seed) throws SeedException {
    final BaseSplittableRandomAdapter adapter = tryCreateRng();
    adapter.setSeed(seed);
    return adapter;
  }

  @Test
  public void testGetSplittableRandom() throws Exception {
    // TODO
  }

  @Override
  @Test
  public void testSerializable() throws SeedException {
    final BaseSplittableRandomAdapter adapter = tryCreateRng();
    // May change when serialized and deserialized, but deserializing twice should yield same object
    // and deserialization should be idempotent
    final BaseSplittableRandomAdapter adapter2 = CloneViaSerialization.clone(adapter);
    final BaseSplittableRandomAdapter adapter3 = CloneViaSerialization.clone(adapter);
    final BaseSplittableRandomAdapter adapter4 = CloneViaSerialization.clone(adapter2);
    testEquivalence(adapter2, adapter3, 20);
    testEquivalence(adapter2, adapter4, 20);
  }

  @Override
  @Test
  public void testNullSeed() {
    // No-op.
  }

  @Override
  @Test(enabled = false)
  public void testEquals() {
    // No-op.
  }

  @Override
  @Test(enabled = false)
  public void testHashCode() {
    // No-op.
  }
}