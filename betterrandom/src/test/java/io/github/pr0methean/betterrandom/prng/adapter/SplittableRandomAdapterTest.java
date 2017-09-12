package io.github.pr0methean.betterrandom.prng.adapter;

import static org.testng.Assert.assertFalse;

import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.FakeSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.util.Arrays;

public class SplittableRandomAdapterTest extends SingleThreadSplittableRandomAdapterTest {

  @Override
  protected SplittableRandomAdapter tryCreateRng() throws SeedException {
    return new SplittableRandomAdapter(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  }

  /** This test only ensures that deserialization produces a usable instance. */
  @Override
  public void testSerializable() throws SeedException {
    RandomTestUtils.serializeAndDeserialize(tryCreateRng()).nextInt();
  }

  /**
   * Since reseeding is thread-local, we can't use a {@link io.github.pr0methean.betterrandom.seed.RandomSeederThread}
   * for this test.
   */
  @Override
  public void testReseeding() throws SeedException {
    SeedGenerator fakeSeedGenerator = new FakeSeedGenerator();
    byte[] output1 = new byte[20];
    byte[] output2 = new byte[20];
    SplittableRandomAdapter rng1 = new SplittableRandomAdapter(fakeSeedGenerator);
    SplittableRandomAdapter rng2 = new SplittableRandomAdapter(fakeSeedGenerator);
    rng1.nextBytes(output1);
    rng2.nextBytes(output2);
    rng1.setSeed(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(8));
    rng1.nextBytes(output1);
    rng2.nextBytes(output2);
    assertFalse(Arrays.equals(output1, output2));
  }

  @Override
  public void testSetSeed() {
    // No-op.
  }
  // TODO: Override or add tests for seederThread-safety.
}
