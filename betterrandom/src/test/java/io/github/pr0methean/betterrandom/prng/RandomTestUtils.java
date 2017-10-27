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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertTrue;

import io.github.pr0methean.betterrandom.util.CloneViaSerialization;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.testng.Reporter;

/**
 * Provides methods used for testing the operation of RNG implementations.
 * @author Daniel Dyer
 */
public enum RandomTestUtils {
  ;

  private static final int INSTANCES_TO_HASH = 25;
  private static final int EXPECTED_UNIQUE_HASHES = (int) (0.8 * INSTANCES_TO_HASH);

  /**
   * @param origin Minimum expected value, inclusive.
   * @param bound Maximum expected value, exclusive.
   */
  public static void checkRangeAndEntropy(final BaseRandom prng, final long expectedEntropySpent,
      final Supplier<? extends Number> numberSupplier, final double origin, final double bound,
      final boolean checkEntropy) {
    checkRangeAndEntropy(prng, expectedEntropySpent, numberSupplier, origin, bound,
        checkEntropy ? EntropyCheckMode.EXACT : EntropyCheckMode.OFF);
  }

  public static void checkRangeAndEntropy(final BaseRandom prng, final long expectedEntropySpent,
      final Supplier<? extends Number> numberSupplier, final double origin, final double bound,
      final EntropyCheckMode entropyCheckMode) {
    final long oldEntropy = prng.getEntropyBits();
    final Number output = numberSupplier.get();
    assertTrue(output.doubleValue() >= origin);
    assertTrue(output.doubleValue() < bound);
    if ((entropyCheckMode == EntropyCheckMode.EXACT) || (entropyCheckMode
        == EntropyCheckMode.UPPER_BOUND)) {
      assertGreaterOrEqual(oldEntropy - expectedEntropySpent, prng.getEntropyBits());
    }
    if ((entropyCheckMode == EntropyCheckMode.EXACT) || (entropyCheckMode
        == EntropyCheckMode.LOWER_BOUND)) {
      assertLessOrEqual(oldEntropy - expectedEntropySpent, prng.getEntropyBits());
    }
  }

  private static void assertLessOrEqual(final long expected, final long actual) {
    if (actual > expected) {
      throw new AssertionError(
          String.format("Expected no more than %d but found %d", expected, actual));
    }
  }

  /**
   * @param expectedCount Negative for an endless stream.
   * @param origin Minimum expected value, inclusive.
   * @param bound Maximum expected value, exclusive.
   */
  public static void checkStream(final BaseRandom prng, final long maxEntropySpentPerNumber,
      final Stream<? extends Number> stream, final int expectedCount, final double origin,
      final double bound, final boolean checkEntropyCount) {
    final AtomicLong entropy = new AtomicLong(prng.getEntropyBits());
    final Stream<? extends Number> streamToUse =
        (expectedCount < 0) ? stream.sequential().limit(20) : stream;
    final long count = streamToUse.mapToLong((number) -> {
      assertGreaterOrEqual(origin, number.doubleValue());
      assertLess(bound, number.doubleValue());
      if (checkEntropyCount && !(streamToUse.isParallel())) {
        long newEntropy = prng.getEntropyBits();
        assertGreaterOrEqual(entropy.getAndSet(newEntropy) - maxEntropySpentPerNumber, newEntropy);
      }
      return 1;
    }).sum();
    if (expectedCount >= 0) {
      assertEquals(count, expectedCount);
    }
    if (checkEntropyCount && streamToUse.isParallel()) {
      assertGreaterOrEqual(entropy.get() - (maxEntropySpentPerNumber * count),
          prng.getEntropyBits());
    }
  }

  private static void assertGreaterOrEqual(final long expected, final long actual) {
    if (actual < expected) {
      throw new AssertionError(
          String.format("Expected at least %d but found %d", expected, actual));
    }
  }

  private static void assertGreaterOrEqual(final double expected, final double actual) {
    if (actual < expected) {
      throw new AssertionError(
          String.format("Expected at least %f but found %f", expected, actual));
    }
  }

  private static void assertLess(final double expected, final double actual) {
    if (actual >= expected) {
      throw new AssertionError(
          String.format("Expected less than %f but found %f", expected, actual));
    }
  }

  /**
   * Test that the given parameterless constructor, called twice, doesn't produce RNGs that compare
   * as equal. Also checks for compliance with basic parts of the Object.equals() contract.
   */
  @SuppressWarnings({"EqualsWithItself", "ObjectEqualsNull", "argument.type.incompatible"})
  public static void doEqualsSanityChecks(final Supplier<? extends Random> ctor) {
    final Random rng = ctor.get();
    final Random rng2 = ctor.get();
    assert !(rng.equals(rng2));
    assert rng.equals(rng) : "RNG doesn't compare equal to itself";
    assert !(rng.equals(null)) : "RNG compares equal to null";
    assert !(rng.equals(new Random())) : "RNG compares equal to new Random()";
  }

  /**
   * Test that in a sample of 100 RNGs from the given parameterless constructor, there are at least
   * 90 unique hash codes.
   */
  public static boolean testHashCodeDistribution(final Supplier<? extends Random> ctor) {
    final HashSet<Integer> uniqueHashCodes = new HashSet<>(INSTANCES_TO_HASH);
    for (int i = 0; i < INSTANCES_TO_HASH; i++) {
      uniqueHashCodes.add(ctor.get().hashCode());
    }
    return uniqueHashCodes.size() >= EXPECTED_UNIQUE_HASHES;
  }

  /**
   * Test to ensure that two distinct RNGs with the same seed return the same sequence of numbers
   * and compare as equal.
   * @param rng1 The first RNG.  Its output is compared to that of {@code rng2}.
   * @param rng2 The second RNG.  Its output is compared to that of {@code rng1}.
   * @param iterations The number of values to generate from each RNG and compare.
   * @return true if the two RNGs produce the same sequence of values, false otherwise.
   */
  public static boolean testEquivalence(final Random rng1, final Random rng2,
      final int iterations) {
    for (int i = 0; i < iterations; i++) {
      if (rng1.nextInt() != rng2.nextInt()) {
        return false;
      }
    }
    return true;
  }

  /**
   * This is a rudimentary check to ensure that the output of a given RNG is approximately uniformly
   * distributed.  If the RNG output is not uniformly distributed, this method will return a poor
   * estimate for the value of pi.
   * @param rng The RNG to test.
   * @param iterations The number of random points to generate for use in the calculation.  This
   *     value needs to be sufficiently large in order to produce a reasonably accurate result
   *     (assuming the RNG is uniform). Less than 10,000 is not particularly useful.  100,000 should
   *     be sufficient.
   * @return An approximation of pi generated using the provided RNG.
   */
  private static double calculateMonteCarloValueForPi(final Random rng, final int iterations) {
    // Assumes a quadrant of a circle of radius 1, bounded by a box with
    // sides of length 1.  The area of the square is therefore 1 square unit
    // and the area of the quadrant is (pi * r^2) / 4.
    int totalInsideQuadrant = 0;
    // Generate the specified number of random points and count how many fall
    // within the quadrant and how many do not.  We expect the number of points
    // in the quadrant (expressed as a fraction of the total number of points)
    // to be pi/4.  Therefore pi = 4 * ratio.
    for (int i = 0; i < iterations; i++) {
      final double x = rng.nextDouble();
      final double y = rng.nextDouble();
      if (isInQuadrant(x, y)) {
        ++totalInsideQuadrant;
      }
    }
    // From these figures we can deduce an approximate value for Pi.
    return 4 * ((double) totalInsideQuadrant / iterations);
  }

  /**
   * Uses Pythagoras' theorem to determine whether the specified coordinates fall within the area of
   * the quadrant of a circle of radius 1 that is centered on the origin.
   * @param x The x-coordinate of the point (must be between 0 and 1).
   * @param y The y-coordinate of the point (must be between 0 and 1).
   * @return True if the point is within the quadrant, false otherwise.
   */
  private static boolean isInQuadrant(final double x, final double y) {
    final double distance = Math.sqrt((x * x) + (y * y));
    return distance <= 1;
  }

  /**
   * Generates a sequence of integers from a given random number generator and then calculates the
   * standard deviation of the sample.
   * @param rng The RNG to use.
   * @param maxValue The maximum value for generated integers (values will be in the range [0,
   *     maxValue)).
   * @param iterations The number of values to generate and use in the standard deviation
   *     calculation.
   * @return The standard deviation of the generated sample.
   */
  public static double calculateSampleStandardDeviation(final Random rng, final int maxValue,
      final int iterations) {
    final DescriptiveStatistics stats = new DescriptiveStatistics();
    for (int i = 0; i < iterations; i++) {
      stats.addValue(rng.nextInt(maxValue));
    }
    return stats.getStandardDeviation();
  }

  @SuppressWarnings("unchecked")
  public static <T extends Random> void assertEquivalentWhenSerializedAndDeserialized(final T rng) {
    final T rng2 = CloneViaSerialization.clone(rng);
    assertNotSame(rng, rng2, "Deserialised RNG should be distinct object.");
    // Both RNGs should generate the same sequence.
    assert testEquivalence(rng, rng2, 20) : "Output mismatch after serialisation.";
  }

  public static void assertMonteCarloPiEstimateSane(final Random rng) {
    final double pi = calculateMonteCarloValueForPi(rng, 100000);
    Reporter.log("Monte Carlo value for Pi: " + pi);
    assertEquals(pi, Math.PI, 0.01 * Math.PI,
        "Monte Carlo value for Pi is outside acceptable range:" + pi);
  }

  public enum EntropyCheckMode {
    EXACT,
    UPPER_BOUND,
    LOWER_BOUND,
    OFF
  }
}
