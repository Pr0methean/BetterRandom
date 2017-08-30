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
import static betterrandom.prng.RandomTestUtils.assertEquivalentWhenSerializedAndDeserialized;
import static org.testng.Assert.assertFalse;

import betterrandom.seed.DefaultSeedGenerator;
import betterrandom.seed.SeedException;
import java.io.IOException;
import java.util.Arrays;
import org.testng.annotations.Test;

/**
 * Unit test for the cellular automaton RNG.
 *
 * @author Daniel Dyer
 */
public class MersenneTwisterRandomTest {

  /**
   * Test to ensure that two distinct RNGs with the same seed return the same sequence of numbers.
   */
  @Test(timeOut = 15000)
  public void testRepeatability() throws SeedException {
    MersenneTwisterRandom rng = new MersenneTwisterRandom();
    // Create second RNG using same seed.
    MersenneTwisterRandom duplicateRNG = new MersenneTwisterRandom(rng.getSeed());
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
    MersenneTwisterRandom rng = new MersenneTwisterRandom(
        DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
    RandomTestUtils.assertMonteCarloPiEstimateSane(rng);
  }

  /**
   * Test to ensure that the output from the RNG is broadly as expected.  This will not detect the
   * subtle statistical anomalies that would be picked up by Diehard, but it provides a simple check
   * for major problems with the output.
   */
  @Test(timeOut = 15000, groups = "non-deterministic",
      dependsOnMethods = "testRepeatability")
  public void testStandardDeviation() throws SeedException {
    MersenneTwisterRandom rng = new MersenneTwisterRandom();
    RandomTestUtils.assertStandardDeviationSane(rng);
  }

  /**
   * Make sure that the RNG does not accept seeds that are too small since this could affect the
   * distribution of the output.
   */
  @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testInvalidSeedSize() {
    new MersenneTwisterRandom(new byte[]{1, 2, 3, 4, 5, 6, 7,
        8}); // Need 16 bytes, should cause an IllegalArgumentException.
  }

  /**
   * RNG must not accept a null seed otherwise it will not be properly initialised.
   */
  @SuppressWarnings("argument.type.incompatible")
  @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testNullSeed() {
    new MersenneTwisterRandom((byte[]) null);
  }

  @Test(timeOut = 15000)
  public void testSerializable() throws IOException, ClassNotFoundException, SeedException {
    // Serialise an RNG.
    MersenneTwisterRandom rng = new MersenneTwisterRandom();
    assertEquivalentWhenSerializedAndDeserialized(rng);
  }

  @Test(timeOut = 15000)
  public void testEquals() throws ReflectiveOperationException {
    RandomTestUtils.doEqualsSanityChecks(MersenneTwisterRandom.class.getConstructor());
  }

  @Test(timeOut = 15000)
  public void testHashCode() throws Exception {
    assert RandomTestUtils.testHashCodeDistribution(MersenneTwisterRandom.class.getConstructor())
        : "Too many hashCode collisions";
  }

  @Test(timeOut = 15000)
  public void testReseeding() throws Exception {
    BaseEntropyCountingRandom rng = new MersenneTwisterRandom();
    byte[] oldSeed = rng.getSeed();
    rng.setSeederThread(DEFAULT_SEEDER);
    rng.nextBytes(new byte[20000]);
    Thread.sleep(2000);
    byte[] newSeed = rng.getSeed();
    assertFalse(Arrays.equals(oldSeed, newSeed));
  }
}
