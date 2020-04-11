package io.github.pr0methean.betterrandom.prng.adapter;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.DEFAULT_INSTANCE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.testng.Assert.assertEquals;

import com.google.common.testing.SerializableTester;
import io.github.pr0methean.betterrandom.TestUtils;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import org.mockito.Mockito;
import org.testng.annotations.Test;

public class SplittableRandomAdapterNullTest extends SplittableRandomAdapterTest {
  @Override protected SplittableRandomAdapter createRng() throws SeedException {
    return new SplittableRandomAdapter(getTestSeedGenerator(), null);
  }

  @Override public void testSerializable() throws SeedException {
    final BaseSplittableRandomAdapter adapter =
        new SplittableRandomAdapter(DEFAULT_INSTANCE, null);
    TestUtils.assertEqualAfterSerialization(adapter);
  }

  @Override public void testReseeding() {
    SeedGenerator generator = Mockito.spy(SecureRandomSeedGenerator.DEFAULT_INSTANCE);
    SplittableRandomAdapter random = new SplittableRandomAdapter(generator, null);
    random.nextBytes(new byte[128]);
    Mockito.verify(generator, Mockito.times(1)).generateSeed(anyInt());
    Mockito.verify(generator, Mockito.never()).generateSeed(any(byte[].class));
  }

  /**
   * SplittableRandomAdapter isn't repeatable until its seed has been specified.
   */
  @Override public void testRepeatability() throws SeedException {
    final BaseRandom rng = createRng();
    rng.setSeed(TEST_SEED);
    // Create second RNG using same seed.
    final BaseRandom duplicateRNG = createRng();
    duplicateRNG.setSeed(TEST_SEED);
    RandomTestUtils.assertEquivalent(rng, duplicateRNG, 1000, "Generated sequences do not match");
  }

  @Override @Test public void testRepeatabilityNextGaussian()
      throws SeedException {
    final BaseRandom rng = createRng();
    final byte[] seed = getTestSeedGenerator().generateSeed(getNewSeedLength());
    rng.nextGaussian();
    rng.setSeed(seed);
    // Create second RNG using same seed.
    final BaseRandom duplicateRNG = createRng();
    duplicateRNG.setSeed(seed);
    assertEquals(rng.nextGaussian(), duplicateRNG.nextGaussian());
  }

  @Override public void testSetSeedGeneratorNoOp() {
    createRng().setRandomSeeder(null);
  }
}
