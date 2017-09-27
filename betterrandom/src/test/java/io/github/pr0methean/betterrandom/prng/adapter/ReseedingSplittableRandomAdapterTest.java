package io.github.pr0methean.betterrandom.prng.adapter;

import static io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator.DEFAULT_SEED_GENERATOR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.seed.FakeSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.CloneViaSerialization;
import org.testng.annotations.Test;

public class ReseedingSplittableRandomAdapterTest extends SingleThreadSplittableRandomAdapterTest {

  @Override
  protected ReseedingSplittableRandomAdapter tryCreateRng() throws SeedException {
    return ReseedingSplittableRandomAdapter.getDefaultInstance();
  }

  // FIXME: Why does this need more time than other PRNGs?!
  @Test(timeOut = 120_000)
  @Override
  public void testDistribution() throws SeedException {
    super.testDistribution();
  }

  // FIXME: Why does this need more time than other PRNGs?!
  @Test(timeOut = 120_000)
  @Override
  public void testStandardDeviation() throws SeedException {
    super.testStandardDeviation();
  }

  @Override
  public void testSerializable() throws SeedException {
    final BaseSplittableRandomAdapter adapter = tryCreateRng();
    assertEquals(adapter, CloneViaSerialization.clone(adapter));
  }

  @Override
  @Test(enabled = false)
  public void testRepeatability() {
    // No-op.
  }

  @Override
  @Test(enabled = false)
  public void testReseeding() {
    // No-op.
  }

  /** Test for crashes only, since setSeed is a no-op. */
  @Override
  public void testSetSeed() throws SeedException {
    final BaseRandom prng = createRng();
    prng.nextLong();
    prng.setSeed(DEFAULT_SEED_GENERATOR.generateSeed(8));
    prng.setSeed(BinaryUtils.convertBytesToLong(DEFAULT_SEED_GENERATOR.generateSeed(8)));
    prng.nextLong();
  }

  @Override
  @Test(enabled = false)
  public void testSeedTooShort() {
    // No-op.
  }

  @Override
  @Test(enabled = false)
  public void testSeedTooLong() {
    // No-op.
  }

  @Override
  public void testDump() throws SeedException {
    assertNotEquals(
        ReseedingSplittableRandomAdapter.getInstance(DEFAULT_SEED_GENERATOR).dump(),
        ReseedingSplittableRandomAdapter.getInstance(new FakeSeedGenerator()).dump());
  }

  @Test
  public void testFinalize() throws SeedException {
    ReseedingSplittableRandomAdapter.getInstance(new FakeSeedGenerator());
    Runtime.getRuntime().runFinalization();
  }
}
