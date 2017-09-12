package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.assertMonteCarloPiEstimateSane;
import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.assertStandardDeviationSane;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import io.github.pr0methean.betterrandom.util.LogPreFormatter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import org.testng.ITestResult;
import org.testng.annotations.Test;
import org.testng.util.RetryAnalyzerCount;

public abstract class BaseRandomTest {

  private static final LogPreFormatter LOG = new LogPreFormatter(BaseRandomTest.class);
  private static final int MAX_DUMPED_SEED_LENGTH = 32;
  private static final int TEST_RESEEDING_RETRIES = 3;

  private static final class TestReseedingRetrier extends RetryAnalyzerCount {
    @EntryPoint
    public TestReseedingRetrier() {
      setCount(TEST_RESEEDING_RETRIES);
    }

    @Override
    public boolean retryMethod(ITestResult iTestResult) {
      return true;
    }
  }

  /**
   * Test to ensure that two distinct RNGs with the same seed return the same sequence of numbers.
   */
  @Test(timeOut = 15000)
  public void testRepeatability() throws SeedException {
    final BaseRandom rng = createRng();
    byte[] seed = rng.getSeed();
    if (seed.length <= MAX_DUMPED_SEED_LENGTH) {
      LOG.info("Original seed is %s", BinaryUtils.convertBytesToHexString(seed));
    }
    // Create second RNG using same seed.
    final BaseRandom duplicateRNG = createRng(rng.getSeed());
    if (seed.length <= MAX_DUMPED_SEED_LENGTH) {
      LOG.info("Copy's seed is %s", BinaryUtils.convertBytesToHexString(duplicateRNG.getSeed()));
    }
    assert RandomTestUtils
        .testEquivalence(rng, duplicateRNG, 1000) :
        String.format("Generated sequences do not match between:%n%s%nand:%n%s", rng.dump(), duplicateRNG.dump());
  }

  @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testSeedTooLong() throws GeneralSecurityException, SeedException {
    createRng(
        DefaultSeedGenerator.DEFAULT_SEED_GENERATOR
            .generateSeed(createRng().getNewSeedLength() + 1)); // Should throw an exception.
  }

  protected abstract BaseRandom tryCreateRng() throws SeedException;

  protected abstract BaseRandom createRng(byte[] seed) throws SeedException;

  /**
   * Test to ensure that the output from the RNG is broadly as expected.  This will not detect the
   * subtle statistical anomalies that would be picked up by Diehard, but it provides a simple check
   * for major problems with the output.
   */
  @Test(timeOut = 15000, groups = "non-deterministic")
  public void testDistribution() throws SeedException {
    final BaseRandom rng = createRng();
    assertMonteCarloPiEstimateSane(rng);
  }

  /**
   * Test to ensure that the output from the RNG is broadly as expected.  This will not detect the
   * subtle statistical anomalies that would be picked up by Diehard, but it provides a simple check
   * for major problems with the output.
   */
  @Test(timeOut = 15000, groups = "non-deterministic")
  public void testStandardDeviation() throws SeedException {
    final BaseRandom rng = createRng();
    // Expected standard deviation for a uniformly distributed population of values in the range 0..n
    // approaches n/sqrt(12).
    assertStandardDeviationSane(rng);
  }

  /**
   * Make sure that the RNG does not accept seeds that are too small since this could affect the
   * distribution of the output.
   */
  @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testSeedTooShort() throws SeedException {
    createRng(
        new byte[]{1, 2, 3}); // One byte too few, should cause an IllegalArgumentException.
  }

  /**
   * RNG must not accept a null seed otherwise it will not be properly initialised.
   */
  @SuppressWarnings("argument.type.incompatible")
  @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testNullSeed() throws SeedException {
    createRng(null);
  }

  @Test(timeOut = 15000)
  public void testSerializable() throws IOException, ClassNotFoundException, SeedException {
    // Serialise an RNG.
    final BaseRandom rng = createRng();
    RandomTestUtils.assertEquivalentWhenSerializedAndDeserialized(rng);
  }

  @Test(timeOut = 15000)
  public void testSetSeed() throws SeedException {
    final byte[] seed = DefaultSeedGenerator.DEFAULT_SEED_GENERATOR
        .generateSeed(createRng().getNewSeedLength());
    final BaseRandom rng = createRng();
    final BaseRandom rng2 = createRng();
    rng.nextLong(); // ensure they won't both be in initial state before reseeding
    rng.setSeed(seed);
    rng2.setSeed(seed);
    assert RandomTestUtils
        .testEquivalence(rng, rng2, 20) : "Output mismatch after reseeding with same seed";
  }

  @Test(timeOut = 15000)
  public void testEquals() throws SeedException {
    RandomTestUtils.doEqualsSanityChecks(this::createRng);
  }

  @Test(timeOut = 60000)
  public void testHashCode() throws Exception {
    assert RandomTestUtils.testHashCodeDistribution(this::createRng)
        : "Too many hashCode collisions";
  }

  /**
   * dump() doesn't have much of a contract, but we should at least expect it to output enough state
   * for two independently-generated instances to give unequal dumps.
   */
  @Test(timeOut = 15000)
  public void testDump() throws SeedException {
    assertNotEquals(createRng().dump(), createRng().dump());
  }

  protected BaseRandom createRng() {
    try {
      return tryCreateRng();
    } catch (final SeedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test(timeOut = 20000, retryAnalyzer = TestReseedingRetrier.class)
  public void testReseeding() throws Exception {
    final BaseRandom rng = createRng();
    rng.setSeederThread(RandomTestUtils.DEFAULT_SEEDER);
    final byte[] oldSeed = rng.getSeed();
    rng.nextBytes(new byte[20000]);
    Thread.sleep(5000);
    final byte[] newSeed = rng.getSeed();
    assertFalse(Arrays.equals(oldSeed, newSeed));
  }

  @Test(timeOut = 1000)
  public void testWithProbability() {
    final BaseRandom prng = createRng();
    final long originalEntropy = prng.entropyBits();
    assertFalse(prng.withProbability(0.0));
    assertTrue(prng.withProbability(1.0));
    assertEquals(originalEntropy, prng.entropyBits());
    prng.withProbability(0.5);
    if (originalEntropy >= 53) {
      assertEquals(originalEntropy - 1, prng.entropyBits());
    }
  }
}
