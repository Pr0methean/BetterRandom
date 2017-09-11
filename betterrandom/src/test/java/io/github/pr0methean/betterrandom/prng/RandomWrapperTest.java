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
package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedException;
import org.testng.annotations.Test;

/**
 * Unit test for the JDK RNG.
 *
 * @author Daniel Dyer
 */
public class RandomWrapperTest {

  /**
   * Test to ensure that two distinct RNGs with the same seed return the same sequence of numbers.
   */
  @Test(timeOut = 15000)
  public void testRepeatability() throws SeedException {
    // Create an RNG using the default seeding strategy.
    final RandomWrapper rng = new RandomWrapper();
    // Create second RNG using same seed.
    final RandomWrapper duplicateRNG = new RandomWrapper(rng.getSeed());
    assert RandomTestUtils
        .testEquivalence(rng, duplicateRNG, 1000) : "Generated sequences do not match.";
  }

  /**
   * Make sure that the RNG does not accept seeds that are too small since this could affect the
   * distribution of the output.
   */
  @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testInvalidSeedSize() {
    new RandomWrapper(new byte[]{1, 2, 3, 4, 5, 6,
        7}); // One byte too few, should cause an IllegalArgumentException.
  }

  /**
   * RNG must not accept a null seed otherwise it will not be properly initialised.
   */
  @SuppressWarnings({"argument.type.incompatible", "return.type.incompatible"})
  @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testNullSeed() throws SeedException {
    new RandomWrapper((byte[]) null);
  }

  @Test(timeOut = 15000)
  public void testEquals() throws ReflectiveOperationException {
    RandomTestUtils.doEqualsSanityChecks(() -> {
      try {
        return new RandomWrapper();
      } catch (final SeedException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test(timeOut = 60000)
  public void testHashCode() throws Exception {
    assert RandomTestUtils.testHashCodeDistribution(() -> {
      try {
        return new RandomWrapper();
      } catch (final SeedException e) {
        throw new RuntimeException(e);
      }
    })
        : "Too many hashCode collisions";
  }

  // Don't bother testing the distribution of the output for this RNG, it's beyond our control.
}
