package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.TestUtils.assertGreaterOrEqual;
import static io.github.pr0methean.betterrandom.TestUtils.assertLessOrEqual;
import static io.github.pr0methean.betterrandom.prng.BaseRandom.ENTROPY_OF_DOUBLE;
import static io.github.pr0methean.betterrandom.prng.BaseRandom.ENTROPY_OF_FLOAT;
import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.assertMonteCarloPiEstimateSane;
import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.checkRangeAndEntropy;
import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.checkStream;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.pr0methean.betterrandom.TestUtils;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java8.util.concurrent.ForkJoinPool;
import java8.util.concurrent.ForkJoinTask;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.function.Supplier;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public abstract class BaseRandomTest {

  protected static final Consumer<BaseRandom> VERIFY_NEXT_INT_NO_CRASH =
      new Consumer<BaseRandom>() {
        @Override public void accept(BaseRandom prng) {
          prng.nextInt();
        }
      };
  /**
   * The square root of 12, rounded from an extended-precision calculation that was done by Wolfram
   * Alpha (and thus at least as accurate as {@code StrictMath.sqrt(12.0)}).
   */
  protected static final double SQRT_12 = 3.4641016151377546;
  protected static final long TEST_SEED = 0x0123456789ABCDEFL;
  protected static final NamedFunction<Random, Double> NEXT_LONG =
      new NamedFunction<Random, Double>("Random::nextLong") {
        @Override public Double apply(Random rng) {
          return (double) (rng.nextLong());
        }
      };
  protected static final NamedFunction<Random, Double> NEXT_INT =
      new NamedFunction<Random, Double>("Random::nextInt") {
        @Override public Double apply(Random rng) {
          return Double.valueOf(rng.nextInt());
        }
      };
  protected static final NamedFunction<Random, Double> NEXT_DOUBLE =
      new NamedFunction<Random, Double>("Random::nextDouble") {
        @Override public Double apply(Random rng) {
          return rng.nextDouble();
        }
      };
  protected static final NamedFunction<Random, Double> NEXT_GAUSSIAN =
      new NamedFunction<Random, Double>("Random::nextGaussian") {
        @Override public Double apply(Random rng) {
          return rng.nextGaussian();
        }
      };
  protected static final List<NamedFunction<Random, Double>> FUNCTIONS_FOR_THREAD_SAFETY_TEST =
      ImmutableList.of(NEXT_LONG, NEXT_INT, NEXT_DOUBLE, NEXT_GAUSSIAN);
  private static final int FLAKY_TEST_RETRIES = 2;
  private static final int TEST_BYTE_ARRAY_LENGTH = 20;
  private static final String HELLO = "Hello";
  private static final String HOW_ARE_YOU = "How are you?";
  private static final String GOODBYE = "Goodbye";
  private static final String[] STRING_ARRAY = {HELLO, HOW_ARE_YOU, GOODBYE};
  @SuppressWarnings("StaticCollection") private static final List<String> STRING_LIST =
      Collections.unmodifiableList(Arrays.asList(STRING_ARRAY));
  private static final int ELEMENTS = 100;
  private static final double UPPER_BOUND_FOR_ROUNDING_TEST =
      Double.longBitsToDouble(Double.doubleToLongBits(1.0) + 4);
  protected final ForkJoinPool pool = new ForkJoinPool(2);
  protected final ConcurrentSkipListSet<Double> sequentialOutput = new ConcurrentSkipListSet<>();
  protected final ConcurrentSkipListSet<Double> parallelOutput = new ConcurrentSkipListSet<>();

  @SafeVarargs
  private static <E> void testGeneratesAll(final Supplier<E> generator, final E... expected) {
    final E[] selected = Arrays.copyOf(expected, ELEMENTS); // Saves passing in a Class<E>
    for (int i = 0; i < ELEMENTS; i++) {
      selected[i] = generator.get();
    }
    assertTrue(Arrays.asList(selected).containsAll(Arrays.asList(expected)));
  }

  @BeforeClass public void setUp() {
    RandomSeederThread.setLoggingEnabled(false);
  }

  @Test public void testAllPublicConstructors()
      throws SeedException, IllegalAccessException, InstantiationException,
      InvocationTargetException {
    TestUtils
        .testAllPublicConstructors(getClassUnderTest(), ImmutableMap.copyOf(constructorParams()),
            new Consumer<BaseRandom>() {
              @Override public void accept(BaseRandom baseRandom) {
                baseRandom.nextInt();
              }
            });
  }

  protected Map<Class<?>, Object> constructorParams() {
    int seedLength = getNewSeedLength(createRng());
    HashMap<Class<?>, Object> params = new HashMap<>();
    params.put(int.class, seedLength);
    params.put(long.class, TEST_SEED);
    params.put(byte[].class, DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(seedLength));
    params.put(SeedGenerator.class, DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
    return params;
  }

  protected int getNewSeedLength(final BaseRandom basePrng) {
    return basePrng.getNewSeedLength();
  }

  protected abstract Class<? extends BaseRandom> getClassUnderTest();

  /**
   * Test to ensure that two distinct RNGs with the same seed return the same sequence of numbers.
   */
  @Test(timeOut = 15000) public void testRepeatability() throws SeedException {
    final BaseRandom rng = createRng();
    // Create second RNG using same seed.
    final BaseRandom duplicateRNG = createRng(rng.getSeed());
    assert RandomTestUtils.testEquivalence(rng, duplicateRNG, 1000) : String
        .format("Generated sequences do not match between:%n%s%nand:%n%s", rng.dump(),
            duplicateRNG.dump());
  }

  @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testSeedTooLong() throws GeneralSecurityException, SeedException {
    createRng(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR
        .generateSeed(getNewSeedLength(createRng()) + 1)); // Should throw an exception.
  }

  protected abstract BaseRandom createRng();

  protected abstract BaseRandom createRng(byte[] seed);

  /**
   * Test to ensure that the output from the RNG is broadly as expected.  This will not detect the
   * subtle statistical anomalies that would be picked up by Diehard, but it provides a simple check
   * for major problems with the output.
   */
  @Test(timeOut = 20000, groups = "non-deterministic") public void testDistribution()
      throws SeedException {
    final BaseRandom rng = createRng();
    assertMonteCarloPiEstimateSane(rng);
  }

  /**
   * Test to ensure that the output from the RNG is broadly as expected.  This will not detect the
   * subtle statistical anomalies that would be picked up by Diehard, but it provides a simple check
   * for major problems with the output.
   */
  @Test(timeOut = 30_000, groups = "non-deterministic") public void testIntegerSummaryStats()
      throws SeedException {
    final BaseRandom rng = createRng();
    // Expected standard deviation for a uniformly distributed population of values in the range 0..n
    // approaches n/sqrt(12).
    // Expected standard deviation for a uniformly distributed population of values in the range 0..n
    // approaches n/sqrt(12).
    for (long n : new long[]{100, 1L << 32, Long.MAX_VALUE}) {
      final int iterations = 10000;
      final SynchronizedDescriptiveStatistics stats =
          RandomTestUtils.summaryStats(rng, n, iterations);
      final double observedSD = stats.getStandardDeviation();
      final double expectedSD = n / SQRT_12;
      Reporter.log("Expected SD: " + expectedSD + ", observed SD: " + observedSD);
      assertGreaterOrEqual(observedSD, 0.98 * expectedSD);
      assertLessOrEqual(observedSD, 1.02 * expectedSD);
      assertGreaterOrEqual(stats.getMax(), 0.9 * n);
      assertLessOrEqual(stats.getMax(), n - 1);
      assertGreaterOrEqual(stats.getMin(), 0);
      assertLessOrEqual(stats.getMin(), 0.1 * n);
      assertGreaterOrEqual(stats.getMean(), 0.4 * n);
      assertLessOrEqual(stats.getMean(), 0.6 * n);
      final double median = stats.getPercentile(50);
      assertGreaterOrEqual(median, 0.4 * n);
      assertLessOrEqual(median, 0.6 * n);
    }
  }

  /**
   * Test to ensure that the output from nextGaussian is broadly as expected.
   */
  @Test(timeOut = 20_000, groups = "non-deterministic") public void testNextGaussianStatistically()
      throws SeedException {
    final BaseRandom rng = createRng();
    final int iterations = 10000;
    final SynchronizedDescriptiveStatistics stats = new SynchronizedDescriptiveStatistics();
    for (int i = 0; i < iterations; i++) {
      stats.addValue(rng.nextGaussian());
    }
    final double observedSD = stats.getStandardDeviation();
    Reporter.log("Expected SD for Gaussians: 1, observed SD: " + observedSD);
    assertGreaterOrEqual(observedSD, 0.97);
    assertLessOrEqual(observedSD, 1.03);
    assertGreaterOrEqual(stats.getMax(), 2.0);
    assertLessOrEqual(stats.getMin(), -2.0);
    assertGreaterOrEqual(stats.getMean(), -0.05);
    assertLessOrEqual(stats.getMean(), 0.05);
    final double median = stats.getPercentile(50);
    assertGreaterOrEqual(median, -0.05);
    assertLessOrEqual(median, 0.05);
  }

  /**
   * Make sure that the RNG does not accept seeds that are too small since this could affect the
   * distribution of the output.
   */
  @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testSeedTooShort() throws SeedException {
    createRng(new byte[]{1, 2, 3}); // One byte too few, should cause an IllegalArgumentException.
  }

  /**
   * RNG must not accept a null seed otherwise it will not be properly initialised.
   */
  @SuppressWarnings("argument.type.incompatible")
  @Test(timeOut = 15000, expectedExceptions = IllegalArgumentException.class)
  public void testNullSeed() throws SeedException {
    createRng(null);
  }

  @Test(timeOut = 15000) public void testSerializable()
      throws IOException, ClassNotFoundException, SeedException {
    // Serialise an RNG.
    final BaseRandom rng = createRng();
    RandomTestUtils.assertEquivalentWhenSerializedAndDeserialized(rng);
  }

  @Test(timeOut = 15000) public void testSetSeed() throws SeedException {
    final byte[] seed =
        DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(getNewSeedLength(createRng()));
    final BaseRandom rng = createRng();
    final BaseRandom rng2 = createRng();
    rng.nextLong(); // ensure they won't both be in initial state before reseeding
    rng.setSeed(seed);
    rng2.setSeed(seed);
    assert RandomTestUtils.testEquivalence(rng, rng2, 20)
        : "Output mismatch after reseeding with same seed";
  }

  @Test(timeOut = 15000) public void testEquals() throws SeedException {
    RandomTestUtils.doEqualsSanityChecks(new Supplier<Random>() {
      @Override public Random get() {
        return BaseRandomTest.this.createRng();
      }
    });
  }

  @Test(timeOut = 60000) public void testHashCode() throws Exception {
    assert RandomTestUtils.testHashCodeDistribution(new Supplier<Random>() {
      @Override public Random get() {
        return BaseRandomTest.this.createRng();
      }
    }) : "Too many hashCode collisions";
  }

  /**
   * dump() doesn't have much of a contract, but we should at least expect it to output enough state
   * for two independently-generated instances to give unequal dumps.
   */
  @Test(timeOut = 15000) public void testDump() throws SeedException {
    final BaseRandom rng = createRng();
    assertNotEquals(rng.dump(), createRng().dump());
    rng.nextLong(); // Kill a mutant where dump doesn't unlock the lock
  }

  @Test public void testReseeding() throws SeedException {
    final byte[] output1 = new byte[20];
    final BaseRandom rng1 = createRng();
    final BaseRandom rng2 = createRng();
    rng1.nextBytes(output1);
    final byte[] output2 = new byte[20];
    rng2.nextBytes(output2);
    final int seedLength = rng1.getNewSeedLength();
    rng1.setSeed(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(seedLength));
    assertGreaterOrEqual(rng1.getEntropyBits(), seedLength * 8L);
    rng1.nextBytes(output1);
    rng2.nextBytes(output2);
    assertFalse(Arrays.equals(output1, output2));
  }

  @Test(timeOut = 60000) public void testRandomSeederThreadIntegration() throws Exception {
    final BaseRandom rng = createRng();
    final byte[] oldSeed = rng.getSeed();
    while (rng.getEntropyBits() > Long.SIZE) {
      rng.nextLong();
    }
    rng.setSeedGenerator(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
    try {
      byte[] newSeed;
      do {
        rng.nextBoolean();
        Thread.sleep(100);
        newSeed = rng.getSeed();
      } while (Arrays.equals(newSeed, oldSeed));
      assertGreaterOrEqual(rng.getEntropyBits(), newSeed.length * 8L - 1);
    } finally {
      rng.setSeedGenerator(null);
    }
  }

  @Test(timeOut = 3000) public void testWithProbability() {
    final BaseRandom prng = createRng();
    final long originalEntropy = prng.getEntropyBits();
    assertFalse(prng.withProbability(0.0));
    assertTrue(prng.withProbability(1.0));
    assertEquals(originalEntropy, prng.getEntropyBits());
    checkRangeAndEntropy(prng, 1, new Supplier<Number>() {
      @Override public Number get() {
        return prng.withProbability(0.7) ? 0 : 1;
      }
    }, 0, 2, true);
  }

  @Test(timeOut = 20_000, groups = "non-deterministic")
  public void testWithProbabilityStatistically() {
    final BaseRandom prng = createRng();
    int trues = 0;
    for (int i = 0; i < 3000; i++) {
      if (prng.withProbability(0.6)) {
        trues++;
      }
    }
    // Significance test at p=3.15E-6 (unadjusted for the multiple subclasses and environments!)
    assertGreaterOrEqual(trues, 1675);
    assertLessOrEqual(trues, 1925);
  }

  @Test(timeOut = 20_000, groups = "non-deterministic") public void testNextBooleanStatistically() {
    final BaseRandom prng = createRng();
    int trues = 0;
    for (int i = 0; i < 3000; i++) {
      if (prng.nextBoolean()) {
        trues++;
      }
    }
    // Significance test at p=4.54E-6 (unadjusted for the multiple subclasses and environments!)
    assertGreaterOrEqual(trues, 1375);
    assertLessOrEqual(trues, 1625);
  }

  @Test public void testNextBytes() throws Exception {
    final byte[] testBytes = new byte[TEST_BYTE_ARRAY_LENGTH];
    final BaseRandom prng = createRng();
    final long oldEntropy = prng.getEntropyBits();
    prng.nextBytes(testBytes);
    assertFalse(Arrays.equals(testBytes, new byte[TEST_BYTE_ARRAY_LENGTH]));
    assertEquals(prng.getEntropyBits(), oldEntropy - (8 * TEST_BYTE_ARRAY_LENGTH));
  }

  @Test public void testNextInt1() throws Exception {
    final BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, 31, new Supplier<Number>() {
      @Override public Number get() {
        return prng.nextInt(3 << 29);
      }
    }, 0, 3 << 29, true);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNextInt1InvalidBound() {
    createRng().nextInt(0);
  }

  @Test public void testNextInt() throws Exception {
    final BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, 32, new Supplier<Number>() {
      @Override public Number get() {
        return prng.nextInt();
      }
    }, Integer.MIN_VALUE, Integer.MAX_VALUE + 1L, true);
  }

  @Test public void testNextInt2() throws Exception {
    final BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, 29, new Supplier<Number>() {
      @Override public Number get() {
        return prng.nextInt(1 << 27, 1 << 29);
      }
    }, 1 << 27, 1 << 29, true);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNextInt2InvalidBound() {
    createRng().nextInt(1, 1);
  }

  @Test public void testNextInt2HugeRange() throws Exception {
    final BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, 32, new Supplier<Number>() {
      @Override public Number get() {
        return prng.nextInt(Integer.MIN_VALUE, 1 << 29);
      }
    }, Integer.MIN_VALUE, 1 << 29, true);
  }

  @Test public void testNextLong() throws Exception {
    final BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, 64, new Supplier<Number>() {
      @Override public Number get() {
        return prng.nextLong();
      }
    }, Long.MIN_VALUE, Long.MAX_VALUE + 1.0, true);
  }

  @Test public void testNextLong1() throws Exception {
    final BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, 42, new Supplier<Number>() {
      @Override public Number get() {
        return prng.nextLong(1L << 42);
      }
    }, 0, 1L << 42, true);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNextLong1InvalidBound() {
    createRng().nextLong(-1);
  }

  @Test public void testNextLong2() throws Exception {
    final BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, 42, new Supplier<Number>() {
      @Override public Number get() {
        return prng.nextLong(1L << 40, 1L << 42);
      }
    }, 1L << 40, 1L << 42, true);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNextLong2InvalidBound() {
    createRng().nextLong(10, 9);
  }

  @Test public void testNextLong2HugeRange() throws Exception {
    final BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, 64, new Supplier<Number>() {
      @Override public Number get() {
        return prng.nextLong(Long.MIN_VALUE, 1 << 29);
      }
    }, Long.MIN_VALUE, 1 << 29, true);
  }

  @Test public void testNextDouble() throws Exception {
    final BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, ENTROPY_OF_DOUBLE, new Supplier<Number>() {
      @Override public Number get() {
        return prng.nextDouble();
      }
    }, 0.0, 1.0, true);
  }

  @Test public void testNextFloat() throws Exception {
    final BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, ENTROPY_OF_FLOAT, new Supplier<Number>() {
      @Override public Number get() {
        return prng.nextFloat();
      }
    }, 0.0, 1.0, true);
  }

  @Test public void testNextDouble1() throws Exception {
    final BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, ENTROPY_OF_DOUBLE, new Supplier<Number>() {
      @Override public Number get() {
        return prng.nextDouble(13.37);
      }
    }, 0.0, 13.37, true);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNextDouble1InvalidBound() {
    createRng().nextDouble(-1.0);
  }

  @Test public void testNextDouble2() throws Exception {
    final BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, ENTROPY_OF_DOUBLE, new Supplier<Number>() {
      @Override public Number get() {
        return prng.nextDouble(-1.0, 13.37);
      }
    }, -1.0, 13.37, true);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNextDouble2InvalidBound() {
    createRng().nextDouble(3.5, 3.5);
  }

  @Test public void testNextGaussian() throws Exception {
    final BaseRandom prng = createRng();
    // TODO: Find out the actual Shannon entropy of nextGaussian() and adjust the entropy count to
    // it in a wrapper function.
    checkRangeAndEntropy(prng, 2 * ENTROPY_OF_DOUBLE, new Supplier<Number>() {
      @Override public Number get() {
        return prng.nextGaussian() + prng.nextGaussian();
      }
    }, -Double.MAX_VALUE, Double.MAX_VALUE, EntropyCheckMode.EXACT);
  }

  @Test public void testNextBoolean() throws Exception {
    final BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, 1, new Supplier<Number>() {
      @Override public Number get() {
        return prng.nextBoolean() ? 0 : 1;
      }
    }, 0, 2, true);
  }

  @Test public void testInts() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, 32, prng.ints().boxed(), -1, Integer.MIN_VALUE, Integer.MAX_VALUE + 1L, true);
  }

  @Test public void testInts1() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, 32, prng.ints(20).boxed(), 20, Integer.MIN_VALUE, Integer.MAX_VALUE + 1L,
        true);
  }

  @Test public void testInts2() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, 29, prng.ints(1 << 27, 1 << 29).boxed(), -1, 1 << 27, 1 << 29, true);
  }

  @Test public void testInts3() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, 29, prng.ints(3, 1 << 27, 1 << 29).boxed(), 3, 1 << 27, 1 << 29, true);
  }

  @Test public void testLongs() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, 64, prng.longs().boxed(), -1, Long.MIN_VALUE, Long.MAX_VALUE + 1.0, true);
  }

  @Test public void testLongs1() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, 64, prng.longs(20).boxed(), 20, Long.MIN_VALUE, Long.MAX_VALUE + 1.0, true);
  }

  @Test public void testLongs2() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, 42, prng.longs(1L << 40, 1L << 42).boxed(), -1, 1L << 40, 1L << 42, true);
  }

  @Test public void testLongs3() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, 42, prng.longs(20, 1L << 40, 1L << 42).boxed(), 20, 1L << 40, 1L << 42, true);
  }

  @Test(timeOut = 1000) public void testLongs3_smallRange() throws Exception {
    final long bound = (1L << 40) + 2;
    final BaseRandom prng = createRng();
    checkStream(prng, 31, prng.longs(20, 1L << 40, bound).boxed(), 20, 1L << 40, bound, true);
  }

  @Test public void testDoubles() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, ENTROPY_OF_DOUBLE, prng.doubles().boxed(), -1, 0.0, 1.0, true);
  }

  @Test public void testDoubles1() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, ENTROPY_OF_DOUBLE, prng.doubles(20).boxed(), 20, 0.0, 1.0, true);
  }

  @Test public void testDoubles2() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, ENTROPY_OF_DOUBLE, prng.doubles(-5.0, 8.0).boxed(), -1, -5.0, 8.0, true);
  }

  @Test public void testDoubles3() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, ENTROPY_OF_DOUBLE, prng.doubles(20, -5.0, 8.0).boxed(), 20, -5.0, 8.0, true);
  }

  @Test public void testDoubles3RoundingCorrection() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, ENTROPY_OF_DOUBLE,
        prng.doubles(20, 1.0, UPPER_BOUND_FOR_ROUNDING_TEST).boxed(), 20, -5.0, 8.0, true);
  }

  @Test public void testGaussians() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, ENTROPY_OF_DOUBLE, prng.gaussians().boxed(), -1, -Double.MAX_VALUE,
        Double.MAX_VALUE, true);
  }

  @Test public void testGaussians1() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, ENTROPY_OF_DOUBLE, prng.gaussians(100).boxed(), 100, -Double.MAX_VALUE,
        Double.MAX_VALUE, true);
  }

  @Test public void testNextElementArray() {
    final BaseRandom prng = createRng();
    testGeneratesAll(new Supplier<String>() {
      @Override public String get() {
        return prng.nextElement(STRING_ARRAY);
      }
    }, STRING_ARRAY);
  }

  @Test public void testNextElementList() {
    final BaseRandom prng = createRng();
    testGeneratesAll(new Supplier<String>() {
      @Override public String get() {
        return prng.nextElement(STRING_LIST);
      }
    }, STRING_ARRAY);
  }

  @Test public void testNextEnum() {
    final BaseRandom prng = createRng();
    testGeneratesAll(new Supplier<TestEnum>() {
      @Override public TestEnum get() {
        return prng.nextEnum(TestEnum.class);
      }
    }, TestEnum.RED, TestEnum.YELLOW, TestEnum.BLUE);
  }

  @Test public void testThreadSafety() {
    testThreadSafety(FUNCTIONS_FOR_THREAD_SAFETY_TEST, FUNCTIONS_FOR_THREAD_SAFETY_TEST);
  }

  protected void testThreadSafetyVsCrashesOnly(List<NamedFunction<Random, Double>> functions) {
    int seedLength = createRng().getNewSeedLength();
    byte[] seed = DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(seedLength);
    for (NamedFunction<Random, Double> supplier1 : functions) {
      for (NamedFunction<Random, Double> supplier2 : functions) {
        runParallel(supplier1, supplier2, seed);
      }
    }
  }

  protected void testThreadSafety(List<NamedFunction<Random, Double>> functions,
      List<NamedFunction<Random, Double>> pairwiseFunctions) {
    int seedLength = createRng().getNewSeedLength();
    byte[] seed = DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(seedLength);
    for (NamedFunction<Random, Double> supplier : functions) {
      for (int i = 0; i < 5; i++) {
        // This loop is necessary to control the false pass rate, especially during mutation testing.
        runSequential(supplier, supplier, seed);
        runParallel(supplier, supplier, seed);
        assertEquals(sequentialOutput, parallelOutput,
            "output differs between sequential/parallel calls to " + supplier);
      }
    }

    // Check that each pair won't crash no matter which order they start in
    // (this part is assertion-free because we can't tell whether A-bits-as-long and
    // B-bits-as-double come from the same bit stream as vice-versa).
    for (NamedFunction<Random, Double> supplier1 : pairwiseFunctions) {
      for (NamedFunction<Random, Double> supplier2 : pairwiseFunctions) {
        if (supplier1 != supplier2) {
          runParallel(supplier2, supplier1, seed);
        }
      }
    }
  }

  protected void runParallel(NamedFunction<Random, Double> supplier1,
      NamedFunction<Random, Double> supplier2, byte[] seed) {
    Random parallelPrng = createRng(seed);
    parallelOutput.clear();
    pool.execute(new GeneratorForkJoinTask(parallelPrng, parallelOutput, supplier1));
    pool.execute(new GeneratorForkJoinTask(parallelPrng, parallelOutput, supplier2));
    assertTrue(pool.awaitQuiescence(10, TimeUnit.SECONDS),
        String.format("Timed out waiting for %s and %s to finish", supplier1, supplier2));
  }

  protected void runSequential(NamedFunction<Random, Double> supplier1,
      NamedFunction<Random, Double> supplier2, byte[] seed) {
    Random sequentialPrng = createRng(seed);
    sequentialOutput.clear();
    new GeneratorForkJoinTask(sequentialPrng, sequentialOutput, supplier1).exec();
    new GeneratorForkJoinTask(sequentialPrng, sequentialOutput, supplier2).exec();
  }

  @AfterClass public void classTearDown() {
    System.gc();
    RandomSeederThread.stopAllEmpty();
  }

  private enum TestEnum {
    RED,
    YELLOW,
    BLUE
  }

  /**
   * ForkJoinTask that reads random longs and adds them to the set.
   */
  protected static final class GeneratorForkJoinTask<T> extends ForkJoinTask<Void> {

    private final Random prng;
    private final ConcurrentSkipListSet<T> set;
    private final Function<Random, T> function;

    public GeneratorForkJoinTask(Random prng, ConcurrentSkipListSet<T> set,
        Function<Random, T> function) {
      this.prng = prng;
      this.set = set;
      this.function = function;
    }

    @Override public Void getRawResult() {
      return null;
    }

    @Override protected void setRawResult(Void value) {
      // No-op.
    }

    @Override protected boolean exec() {
      for (int i = 0; i < 1000; i++) {
        set.add(function.apply(prng));
      }
      return true;
    }
  }

  protected static abstract class NamedFunction<T, R> implements Function<T, R> {

    private final String name;

    public NamedFunction(String name) {
      this.name = name;
    }

    @Override public String toString() {
      return name;
    }
  }
}
