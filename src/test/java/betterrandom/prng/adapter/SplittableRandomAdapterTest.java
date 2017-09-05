package betterrandom.prng.adapter;

import static betterrandom.prng.RandomTestUtils.serializeAndDeserialize;
import static org.testng.Assert.assertEquals;

import betterrandom.seed.DefaultSeedGenerator;
import betterrandom.seed.SeedException;

public class SplittableRandomAdapterTest extends SingleThreadSplittableRandomAdapterTest {

  @Override
  protected SplittableRandomAdapter tryCreateRng() throws SeedException {
    return new SplittableRandomAdapter(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  }

  @Override
  public void testSerializable() throws SeedException {
    BaseSplittableRandomAdapter adapter = tryCreateRng();
    assertEquals(adapter, serializeAndDeserialize(adapter));
  }

  @Override
  public void testSetSeed() {
    // No-op.
  }
  // TODO: Override or add tests for thread-safety.
}
