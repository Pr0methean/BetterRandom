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

import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertBytesToHexString;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import com.google.common.testing.SerializableTester;
import com.google.common.util.concurrent.Uninterruptibles;
import io.github.pr0methean.betterrandom.TestUtils;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeederThread;
import io.github.pr0methean.betterrandom.util.Dumpable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java8.util.function.Consumer;
import java8.util.function.Supplier;
import java8.util.function.ToLongFunction;
import java8.util.stream.BaseStream;
import java8.util.stream.Stream;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.testng.Reporter;

/**
 * Provides methods used for testing the operation of RNG implementations.
 *
 * @author Chris Hennick, Daniel Dyer
 */
public enum RandomTestUtils {
  ;

  private static final int INSTANCES_TO_HASH = 25;
  private static final int EXPECTED_UNIQUE_HASHES = (int) (0.8 * INSTANCES_TO_HASH);
  public static final long RESEEDING_WAIT_INCREMENT_MS = 20L;

  @SuppressWarnings("FloatingPointEquality")
  public static void checkRangeAndEntropy(final BaseRandom prng, final long expectedEntropySpent,
      final Supplier<? extends Number> numberSupplier, final double origin, final double bound,
      final EntropyCheckMode entropyCheckMode) {
    final long oldEntropy = prng.getEntropyBits();
    final Number output = numberSupplier.get();
    TestUtils.assertGreaterOrEqual(output.doubleValue(), origin);
    if ((bound - 1.0) == bound) {
      // Can't do a strict check because of floating-point rounding
      TestUtils.assertLessOrEqual(output.doubleValue(), bound);
    } else {
      TestUtils.assertLess(output.doubleValue(), bound);
    }
    final long entropy = prng.getEntropyBits();
    final long expectedEntropy = oldEntropy - expectedEntropySpent;
    switch (entropyCheckMode) {
      case EXACT:
        assertEquals(entropy, expectedEntropy);
        break;
      case LOWER_BOUND:
        TestUtils.assertGreaterOrEqual(entropy, expectedEntropy);
        break;
      case OFF:
        break;
      default:
        throw new AssertionError("Unhandled EntropyCheckMode " + entropyCheckMode);
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
        (expectedCount < 0) ? stream.sequential().limit(20) : stream.sequential();
    final long count = streamToUse.mapToLong(new ToLongFunction<Number>() {
      @Override public long applyAsLong(Number number) {
        TestUtils.assertGreaterOrEqual(number.doubleValue(), origin);
        TestUtils.assertLess(number.doubleValue(), bound);
        if (checkEntropyCount) {
          long newEntropy = prng.getEntropyBits();
          TestUtils.assertGreaterOrEqual(newEntropy,
              entropy.getAndSet(newEntropy) - maxEntropySpentPerNumber);
        }
        return 1;
      }
    }).sum();
    if (expectedCount >= 0) {
      assertEquals(count, expectedCount);
    }
  }

  /**
   * Test that the given parameterless constructor, called twice, doesn't produce RNGs that compare
   * as equal. Also checks for compliance with basic parts of the Object.equals() contract.
   */
  @SuppressWarnings({"EqualsWithItself", "ObjectEqualsNull"})
  public static void doEqualsSanityChecks(final Supplier<? extends Random> ctor) {
    final Random rng = ctor.get();
    final Random rng2 = ctor.get();
    assertNotEquals(rng, rng2);
    assertEquals(rng, rng, "RNG doesn't compare equal to itself");
    assertNotEquals(rng, null, "RNG compares equal to null");
    assertNotEquals(rng, new Random(), "RNG compares equal to new Random()");
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

  private static void assertEquivalentOrDistinct(final Random rng1, final Random rng2,
      final int iterations, final String message, final boolean shouldBeEquivalent) {
    byte[] out1 = new byte[iterations];
    rng1.nextBytes(out1);
    byte[] out2 = new byte[iterations];
    rng2.nextBytes(out2);
    if (Arrays.equals(out1, out2) != shouldBeEquivalent) {
      final String fullMessage = String
          .format("%s:%n%s -> %s%nvs.%n%s -> %s%n", message, toString(rng1),
              convertBytesToHexString(out1), toString(rng2), convertBytesToHexString(out2));
      throw new AssertionError(fullMessage);
    }
  }

  public static void assertEquivalent(final Random rng1, final Random rng2, final int iterations,
      final String message) {
    assertEquivalentOrDistinct(rng1, rng2, iterations, message, true);
  }

  public static void assertDistinct(final Random rng1, final Random rng2, final int iterations,
      final String message) {
    assertEquivalentOrDistinct(rng1, rng2, iterations, message, false);
  }

  public static String toString(final Random rng) {
    return rng instanceof Dumpable ? ((Dumpable) rng).dump() : rng.toString();
  }

  /**
   * This is a rudimentary check to ensure that the output of a given RNG is approximately uniformly
   * distributed.  If the RNG output is not uniformly distributed, this method will return a poor
   * estimate for the value of pi.
   *
   * @param rng The RNG to test.
   * @param iterations The number of random points to generate for use in the calculation.  This
   *     value needs to be sufficiently large in order to produce a reasonably
   *     accurate result
   *     (assuming the RNG is uniform). Less than 10,000 is not particularly useful
   *     .  100,000 should
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
   *
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
   *
   * @param rng The RNG to use.
   * @param maxValue The maximum value for generated integers (values will be in the range [0,
   *     maxValue)).
   * @param iterations The number of values to generate and use in the standard deviation
   *     calculation.
   * @return The standard deviation of the generated sample.
   */
  public static SynchronizedDescriptiveStatistics summaryStats(final BaseRandom rng,
      final long maxValue, final int iterations) {
    final SynchronizedDescriptiveStatistics stats = new SynchronizedDescriptiveStatistics();
    final BaseStream<? extends Number, ?> stream =
        (maxValue <= Integer.MAX_VALUE) ? rng.ints(iterations, 0, (int) maxValue) :
            rng.longs(iterations, 0, maxValue);
    stream.spliterator().forEachRemaining(new Consumer<Number>() {
      @Override public void accept(Number number) {
        stats.addValue(number.doubleValue());
      }
    });
    return stats;
  }

  @SuppressWarnings("unchecked")
  public static <T extends Random> void assertEquivalentWhenSerializedAndDeserialized(final T rng) {
    final T rng2 = SerializableTester.reserialize(rng);
    assertNotSame(rng, rng2, "Deserialised RNG should be distinct object.");
    // Both RNGs should generate the same sequence.
    assertEquivalent(rng, rng2, 20, "Output mismatch after serialisation.");
    assertEquals(rng.getClass(), rng2.getClass());
  }

  public static void assertMonteCarloPiEstimateSane(final Random rng) {
    final double pi = calculateMonteCarloValueForPi(rng, 100000);
    Reporter.log("Monte Carlo value for Pi: " + pi);
    assertEquals(pi, Math.PI, 0.01 * Math.PI,
        "Monte Carlo value for Pi is outside acceptable range:" + pi);
  }

  public static void testReseeding(final SeedGenerator testSeedGenerator, final BaseRandom rng,
      final boolean setSeedGenerator) {
    // TODO: Set max thread priority
    final byte[] oldSeed = rng.getSeed();
    final byte[] oldSeedClone = oldSeed.clone();
    int maxReseedingWaitIncrements = 1000 + rng.getNewSeedLength() / 4;
    SimpleRandomSeederThread seeder = null;
    if (setSeedGenerator) {
      seeder = new SimpleRandomSeederThread(testSeedGenerator);
      rng.setRandomSeeder(seeder);
    }
    try {
      int bytesToDrain = (int) ((rng.getEntropyBits() / 8) + 1);
      rng.nextBytes(new byte[bytesToDrain]);
      assertTrue(Arrays.equals(oldSeed, oldSeedClone),
          "Array modified after being returned by getSeed()");
      int waits = 0;
      byte[] secondSeed;
      do {
        assertEquals(oldSeedClone, oldSeed, "Array modified after being returned by getSeed()");
        if (setSeedGenerator) {
          assertSame(rng.getRandomSeeder(), seeder);
        }
        waits++;
        if (waits > maxReseedingWaitIncrements) {
          fail(String.format("Timed out waiting for %s to be reseeded!", rng));
        }
        Uninterruptibles.sleepUninterruptibly(RESEEDING_WAIT_INCREMENT_MS, TimeUnit.MILLISECONDS);
        secondSeed = rng.getSeed();
      } while (Arrays.equals(secondSeed, oldSeed));
      final byte[] secondSeedClone = secondSeed.clone();
      waits = 0;
      while (rng.getEntropyBits() < (secondSeed.length * 8L) - 1) {
        assertEquals(secondSeedClone, secondSeed,
            "Array modified after being returned by getSeed()");
        waits++;
        if (waits > 5) {
          fail(String.format("Timed out waiting for entropy count to increase on %s", rng));
        }
        // FIXME: Flaky if we only sleep for 10 ms at a time
        Uninterruptibles.sleepUninterruptibly(100L, TimeUnit.MILLISECONDS);
      }
      byte[] thirdSeed;
      while (rng.getEntropyBits() > 0) {
        rng.nextLong();
      }
      waits = 0;
      do {
        assertEquals(secondSeedClone, secondSeed,
            "Array modified after being returned by getSeed()");
        Uninterruptibles.sleepUninterruptibly(100L, TimeUnit.MILLISECONDS);
        waits++;
        if (waits > maxReseedingWaitIncrements) {
          fail(String.format("Timed out waiting for %s to be reseeded!", rng));
        }
        thirdSeed = rng.getSeed();
      } while (Arrays.equals(thirdSeed, secondSeed));
    } finally {
      if (setSeedGenerator) {
        RandomTestUtils.removeAndAssertEmpty(seeder, rng);
        assertNull(rng.getRandomSeeder());
      }
    }
  }

  public static void removeAndAssertEmpty(final SimpleRandomSeederThread seederThread,
      final BaseRandom prng) {
    prng.setRandomSeeder(null);
    seederThread.remove(prng);
    seederThread.stopIfEmpty();
    assertTrue(seederThread.isEmpty());
  }

  public static void removeAndAssertEmpty(final RandomSeederThread seederThread,
      final Random prng) {
    seederThread.remove(prng);
    seederThread.stopIfEmpty();
    assertTrue(seederThread.isEmpty());
    assertFalse(seederThread.isRunning());
  }

  public enum EntropyCheckMode {
    EXACT, LOWER_BOUND, OFF
  }
}
