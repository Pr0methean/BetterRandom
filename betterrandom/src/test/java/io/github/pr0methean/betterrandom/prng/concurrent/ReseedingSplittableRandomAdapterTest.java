package io.github.pr0methean.betterrandom.prng.concurrent;

import io.github.pr0methean.betterrandom.CloneViaSerialization;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode;
import io.github.pr0methean.betterrandom.seed.*;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

@SuppressWarnings("BusyWait")
public class ReseedingSplittableRandomAdapterTest extends SingleThreadSplittableRandomAdapterTest {

  private RandomSeederThread thread;

  @Override
  protected SeedGenerator getTestSeedGenerator() {
    return semiFakeSeedGenerator;
  }

  @BeforeTest public void setUp() {
    thread = new RandomSeederThread(getTestSeedGenerator());
  }

  @AfterTest public void tearDown() {
    thread.stopIfEmpty();
  }

  @Override protected EntropyCheckMode getEntropyCheckMode() {
    return EntropyCheckMode.LOWER_BOUND;
  }

  @Override protected ReseedingSplittableRandomAdapter createRng() throws SeedException {
    return ReseedingSplittableRandomAdapter.getInstance(thread, getTestSeedGenerator());
  }

  // FIXME: Why does this need more time than other PRNGs?!
  @Test(timeOut = 120_000) @Override public void testDistribution() throws SeedException {
    super.testDistribution();
  }

  // FIXME: Why does this need more time than other PRNGs?!
  @Test(timeOut = 120_000) @Override public void testIntegerSummaryStats() throws SeedException {
    super.testIntegerSummaryStats();
  }

  @Override @Test public void testSerializable() throws SeedException {
    // SemifakeSeedGenerator-based RandomSeederThread can't be used, because SemifakeSeedGenerator doesn't equals() its
    // clone-by-serialization
    RandomSeederThread thread = new RandomSeederThread(SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR);
    try {
      final BaseSplittableRandomAdapter adapter =
          ReseedingSplittableRandomAdapter.getInstance(thread,
              SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR);
      final BaseSplittableRandomAdapter clone = CloneViaSerialization.clone(adapter);
      assertEquals(adapter, clone);
    } finally {
      thread.stopIfEmpty();
    }
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return ReseedingSplittableRandomAdapter.class;
  }

  @Override @Test(enabled = false) public void testRepeatability() {
    // No-op.
  }

  @Override @Test(enabled = false) public void testRepeatabilityNextGaussian() {
    // No-op.
  }

  @SuppressWarnings("BusyWait") @Override @Test(retryAnalyzer = FlakyRetryAnalyzer.class)
  public void testReseeding() {
    RandomTestUtils.testReseeding(getTestSeedGenerator(), createRng(), false);
  }

  /** Test for crashes only, since setSeed is a no-op. */
  @Override @Test public void testSetSeedAfterNextLong() throws SeedException {
    final BaseRandom prng = createRng();
    prng.nextLong();
    prng.setSeed(getTestSeedGenerator().generateSeed(8));
    prng.setSeed(BinaryUtils.convertBytesToLong(getTestSeedGenerator().generateSeed(8)));
    prng.nextLong();
  }

  /** Test for crashes only, since setSeed is a no-op. */
  @Override @Test public void testSetSeedAfterNextInt() throws SeedException {
    final BaseRandom prng = createRng();
    prng.nextInt();
    prng.setSeed(getTestSeedGenerator().generateSeed(8));
    prng.setSeed(BinaryUtils.convertBytesToLong(getTestSeedGenerator().generateSeed(8)));
    prng.nextInt();
  }

  /** Assertion-free since reseeding may cause divergent output. */
  @Override @Test(timeOut = 10000) public void testSetSeedLong() {
    createRng().setSeed(0x0123456789ABCDEFL);
  }

  /** setRandomSeeder doesn't work on this class and shouldn't pretend to. */
  @Override @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testRandomSeederThreadIntegration() {
    RandomSeederThread thread = new RandomSeederThread(SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR);
    try {
      createRng().setRandomSeeder(thread);
    } finally {
      thread.stopIfEmpty();
    }
  }

  @Test public void testSetSeedGeneratorNoOp() {
    ReseedingSplittableRandomAdapter.getInstance(thread, getTestSeedGenerator())
        .setRandomSeeder(thread);
  }

  @Override @Test(enabled = false) public void testSeedTooShort() {
    // No-op.
  }

  @Override @Test(enabled = false) public void testSeedTooLong() {
    // No-op.
  }

  @Override @Test public void testDump() throws SeedException {
    RandomSeederThread thread = new RandomSeederThread(SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR);
    try {
      assertNotEquals(ReseedingSplittableRandomAdapter.getInstance(thread, new FakeSeedGenerator()).dump(),
          ReseedingSplittableRandomAdapter.getInstance(thread, getTestSeedGenerator()).dump());
      RandomSeederThread otherThread = new RandomSeederThread(new FakeSeedGenerator());
      try {
        assertNotEquals(ReseedingSplittableRandomAdapter.getInstance(otherThread, getTestSeedGenerator()).dump(),
            ReseedingSplittableRandomAdapter.getInstance(thread, getTestSeedGenerator()).dump());
      } finally {
        otherThread.stopIfEmpty();
      }
    } finally {
      thread.stopIfEmpty();
    }
  }

  /** Assertion-free because thread-local. */
  @Override @Test public void testThreadSafety() {
    testThreadSafetyVsCrashesOnly(30, functionsForThreadSafetyTest);
  }
}
