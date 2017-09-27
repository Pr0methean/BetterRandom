package io.github.pr0methean.betterrandom.prng.adapter;

import static org.testng.Assert.assertFalse;

import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.FakeSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import java.util.Arrays;

public class SplittableRandomAdapterTest extends SingleThreadSplittableRandomAdapterTest {

  private static final SeedGenerator FAKE_SEED_GENERATOR = new FakeSeedGenerator();
  private static final long TEST_SEED = 0x0123456789ABCDEFL;

  /** SplittableRandomAdapter isn't repeatable until its seed has been specified. */
  @Override
  public void testRepeatability() throws SeedException {
    final BaseRandom rng = createRng();
    rng.setSeed(TEST_SEED);
    // Create second RNG using same seed.
    final BaseRandom duplicateRNG = createRng();
    duplicateRNG.setSeed(TEST_SEED);
    assert RandomTestUtils
        .testEquivalence(rng, duplicateRNG, 1000) :
        String.format("Generated sequences do not match between:%n%s%nand:%n%s", rng.dump(),
            duplicateRNG.dump());
  }

  @Override
  protected SplittableRandomAdapter tryCreateRng() throws SeedException {
    return new SplittableRandomAdapter(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  }

  /**
   * Since reseeding is thread-local, we can't use a {@link io.github.pr0methean.betterrandom.seed.RandomSeederThread}
   * for this test.
   */
  @Override
  public void testReseeding() throws SeedException {
    final byte[] output1 = new byte[20];
    final SplittableRandomAdapter rng1 = new SplittableRandomAdapter(FAKE_SEED_GENERATOR);
    final SplittableRandomAdapter rng2 = new SplittableRandomAdapter(FAKE_SEED_GENERATOR);
    rng1.nextBytes(output1);
    final byte[] output2 = new byte[20];
    rng2.nextBytes(output2);
    rng1.setSeed(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(8));
    rng1.nextBytes(output1);
    rng2.nextBytes(output2);
    assertFalse(Arrays.equals(output1, output2));
  }

  // TODO: Override or add tests for thread-safety.
}
