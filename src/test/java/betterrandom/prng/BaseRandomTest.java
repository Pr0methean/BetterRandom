package betterrandom.prng;

import static betterrandom.prng.RandomTestUtils.assertMonteCarloPiEstimateSane;
import static betterrandom.prng.RandomTestUtils.assertStandardDeviationSane;
import static org.testng.Assert.assertNotEquals;

import betterrandom.seed.DefaultSeedGenerator;
import betterrandom.seed.SeedException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.testng.annotations.Test;

public abstract class BaseRandomTest {

  /**
   * Test to ensure that two distinct RNGs with the same seed return the same sequence of numbers.
   */
  @Test(timeOut = 15000)
  public void testRepeatability() throws SeedException {
    BaseRandom rng = createRng();
    // Create second RNG using same seed.
    BaseRandom duplicateRNG = createRng(rng.getSeed());
    assert RandomTestUtils
        .testEquivalence(rng, duplicateRNG, 1000) : "Generated sequences do not match.";
  }

  protected BaseRandom createRng() {
    try {
      return tryCreateRng();
    } catch (SeedException e) {
      throw new RuntimeException(e);
    }
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
  @Test(timeOut = 15000, groups = "non-deterministic",
      dependsOnMethods = "testRepeatability")
  public void testDistribution() throws SeedException {
    BaseRandom rng = createRng();
    assertMonteCarloPiEstimateSane(rng);
  }

  /**
   * Test to ensure that the output from the RNG is broadly as expected.  This will not detect the
   * subtle statistical anomalies that would be picked up by Diehard, but it provides a simple check
   * for major problems with the output.
   */
  @Test(timeOut = 15000, groups = "non-deterministic",
      dependsOnMethods = "testRepeatability")
  public void testStandardDeviation() throws SeedException {
    BaseRandom rng = createRng();
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
    BaseRandom rng = createRng();
    RandomTestUtils.assertEquivalentWhenSerializedAndDeserialized(rng);
  }

  @Test(timeOut = 15000)
  public void testSetSeed() throws SeedException {
    byte[] seed = DefaultSeedGenerator.DEFAULT_SEED_GENERATOR
        .generateSeed(createRng().getNewSeedLength());
    BaseRandom rng = createRng();
    BaseRandom rng2 = createRng();
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
}
