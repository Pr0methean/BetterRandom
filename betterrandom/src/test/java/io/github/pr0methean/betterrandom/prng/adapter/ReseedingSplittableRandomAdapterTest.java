package io.github.pr0methean.betterrandom.prng.adapter;

import static io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator.DEFAULT_SEED_GENERATOR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.FakeSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.CloneViaSerialization;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListSet;
import org.testng.annotations.Test;

public class ReseedingSplittableRandomAdapterTest extends SingleThreadSplittableRandomAdapterTest {

  @Override protected ReseedingSplittableRandomAdapter createRng() throws SeedException {
    return ReseedingSplittableRandomAdapter.getDefaultInstance();
  }

  // FIXME: Why does this need more time than other PRNGs?!
  @Test(timeOut = 120_000) @Override public void testDistribution() throws SeedException {
    super.testDistribution();
  }

  // FIXME: Why does this need more time than other PRNGs?!
  @Test(timeOut = 120_000) @Override public void testStandardDeviation() throws SeedException {
    super.testStandardDeviation();
  }

  @Override @Test public void testSerializable() throws SeedException {
    final BaseSplittableRandomAdapter adapter = createRng();
    assertEquals(adapter, CloneViaSerialization.clone(adapter));
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return ReseedingSplittableRandomAdapter.class;
  }

  @Override @Test(enabled = false) public void testRepeatability() {
    // No-op.
  }

  @Override @Test(enabled = false) public void testReseeding() {
    // No-op.
  }

  /** Test for crashes only, since setSeed is a no-op. */
  @Override @Test public void testSetSeed() throws SeedException {
    final BaseRandom prng = createRng();
    prng.nextLong();
    prng.setSeed(DEFAULT_SEED_GENERATOR.generateSeed(8));
    prng.setSeed(BinaryUtils.convertBytesToLong(DEFAULT_SEED_GENERATOR.generateSeed(8)));
    prng.nextLong();
  }

  /**
   * This class manages its own interaction with a RandomSeederThread, so setSeederThread makes no
   * sense.
   */
  @Override @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testRandomSeederThreadIntegration() throws Exception {
    createRng().setSeedGenerator(DEFAULT_SEED_GENERATOR);
  }

  @Override @Test(enabled = false) public void testSeedTooShort() {
    // No-op.
  }

  @Override @Test(enabled = false) public void testSeedTooLong() {
    // No-op.
  }

  @Override @Test public void testDump() throws SeedException {
    assertNotEquals(ReseedingSplittableRandomAdapter.getInstance(DEFAULT_SEED_GENERATOR).dump(),
        ReseedingSplittableRandomAdapter.getInstance(new FakeSeedGenerator()).dump());
  }

  @Test public void testFinalize() throws SeedException {
    ReseedingSplittableRandomAdapter.getInstance(new FakeSeedGenerator());
    Runtime.getRuntime().runFinalization();
  }

  /** Assertion-free because thread-local. */
  @Override @Test public void testThreadSafety() {
    ConcurrentSkipListSet<Long> sequentialLongs = new ConcurrentSkipListSet<>();
    ConcurrentSkipListSet<Long> parallelLongs = new ConcurrentSkipListSet<>();
    runSequentialAndParallel(sequentialLongs, parallelLongs, Random::nextLong);
    ConcurrentSkipListSet<Double> sequentialDoubles = new ConcurrentSkipListSet<>();
    ConcurrentSkipListSet<Double> parallelDoubles = new ConcurrentSkipListSet<>();
    runSequentialAndParallel(sequentialDoubles, parallelDoubles, Random::nextDouble);
    sequentialDoubles.clear();
    parallelDoubles.clear();
    runSequentialAndParallel(sequentialDoubles, parallelDoubles, Random::nextGaussian);
    ConcurrentSkipListSet<Integer> sequentialInts = new ConcurrentSkipListSet<>();
    ConcurrentSkipListSet<Integer> parallelInts = new ConcurrentSkipListSet<>();
    runSequentialAndParallel(sequentialInts, parallelInts, Random::nextInt);
  }
}
