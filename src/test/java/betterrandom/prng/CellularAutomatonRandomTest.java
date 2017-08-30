// ============================================================================
//   Copyright 2006-2012 Daniel W. Dyer
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
// ============================================================================
package betterrandom.prng;

import static betterrandom.prng.RandomTestUtils.DEFAULT_SEEDER;
import static betterrandom.prng.RandomTestUtils.assertMonteCarloPiEstimateSane;
import static betterrandom.prng.RandomTestUtils.assertStandardDeviationSane;
import static org.testng.Assert.assertFalse;

import betterrandom.DeadlockWatchdogThread;
import betterrandom.seed.DefaultSeedGenerator;
import betterrandom.seed.SeedException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Unit test for the cellular automaton RNG.
 *
 * @author Daniel Dyer
 */
public class CellularAutomatonRandomTest {

  @BeforeClass
  public void setUp() {
    DeadlockWatchdogThread.ensureStarted();
  }

  /**
   * Test to ensure that two distinct RNGs with the same seed return the same sequence of numbers.
   */
  @Test(timeOut = 15000)
  public void testRepeatability() throws SeedException {
    CellularAutomatonRandom rng = new CellularAutomatonRandom();
    // Create second RNG using same seed.
    CellularAutomatonRandom duplicateRNG = new CellularAutomatonRandom(rng.getSeed());
    assert RandomTestUtils
        .testEquivalence(rng, duplicateRNG, 1000) : "Generated sequences do not match.";
  }

  /**
   * Test to ensure that the output from the RNG is broadly as expected.  This will not detect the
   * subtle statistical anomalies that would be picked up by Diehard, but it provides a simple check
   * for major problems with the output.
   */
  @Test(timeOut = 15000, groups = "non-deterministic",
      dependsOnMethods = "testRepeatability")
  public void testDistribution() throws SeedException {
    CellularAutomatonRandom rng = new CellularAutomatonRandom(
        DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
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
    CellularAutomatonRandom rng = new CellularAutomatonRandom();
    // Expected standard deviation for a uniformly distributed population of values in the range 0..n
    // approaches n/sqrt(12).
    assertStandardDeviationSane(rng);
  }

  /**
   * Make sure that the RNG does not accept seeds that are too small since this could affect the
   * distribution of the output.
   */
  @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testInvalidSeedSize() {
    new CellularAutomatonRandom(
        new byte[]{1, 2, 3}); // One byte too few, should cause an IllegalArgumentException.
  }

  /**
   * RNG must not accept a null seed otherwise it will not be properly initialised.
   */
  @SuppressWarnings("argument.type.incompatible")
  @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testNullSeed() {
    new CellularAutomatonRandom((byte[]) null);
  }

  @Test(timeOut = 15000)
  public void testSerializable() throws IOException, ClassNotFoundException, SeedException {
    // Serialise an RNG.
    CellularAutomatonRandom rng = new CellularAutomatonRandom();
    RandomTestUtils.assertEquivalentWhenSerializedAndDeserialized(rng);
  }

  @Test(timeOut = 15000)
  public void testSetSeed() throws SeedException {
    long seed = new Random().nextLong();
    CellularAutomatonRandom rng = new CellularAutomatonRandom();
    CellularAutomatonRandom rng2 = new CellularAutomatonRandom();
    rng.nextLong(); // ensure they won't both be in initial state before reseeding
    rng.setSeed(seed);
    rng2.setSeed(seed);
    assert RandomTestUtils
        .testEquivalence(rng, rng2, 20) : "Output mismatch after reseeding with same seed";
  }

  @Test(timeOut = 15000)
  public void testEquals() throws ReflectiveOperationException {
    RandomTestUtils.doEqualsSanityChecks(CellularAutomatonRandom.class.getConstructor());
  }

  @Test(timeOut = 30000)
  public void testHashCode() throws Exception {
    assert RandomTestUtils.testHashCodeDistribution(CellularAutomatonRandom.class.getConstructor())
        : "Too many hashCode collisions";
  }

  @Test(timeOut = 15000)
  public void testReseeding() throws Exception {
    BaseEntropyCountingRandom rng = new CellularAutomatonRandom();
    byte[] oldSeed = rng.getSeed();
    rng.setSeederThread(DEFAULT_SEEDER);
    rng.nextBytes(new byte[20000]);
    Thread.sleep(10);
    byte[] newSeed = rng.getSeed();
    assertFalse(Arrays.equals(oldSeed, newSeed));
  }
}
