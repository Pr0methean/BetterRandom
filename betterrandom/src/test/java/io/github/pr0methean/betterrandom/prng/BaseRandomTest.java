package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.TestUtils.assertGreaterOrEqual;
import static io.github.pr0methean.betterrandom.TestUtils.assertLessOrEqual;
import static io.github.pr0methean.betterrandom.prng.BaseRandom.ENTROPY_OF_DOUBLE;
import static io.github.pr0methean.betterrandom.prng.BaseRandom.ENTROPY_OF_FLOAT;
import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.assertMonteCarloPiEstimateSane;
import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.checkRangeAndEntropy;
import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.checkStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.pr0methean.betterrandom.CloneViaSerialization;
import io.github.pr0methean.betterrandom.TestUtils;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode;
import io.github.pr0methean.betterrandom.prng.adapter.SplittableRandomAdapter;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.FakeSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SemiFakeSeedGenerator;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.mockito.mockpolicies.Slf4jMockPolicy;
import org.powermock.core.classloader.annotations.MockPolicy;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.powermock.reflect.Whitebox;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

@MockPolicy(Slf4jMockPolicy.class)
@PrepareForTest(DefaultSeedGenerator.class)
@PowerMockIgnore({"javax.crypto.*", "javax.management.*", "javax.script.*", "jdk.nashorn.*"})
public abstract class BaseRandomTest extends PowerMockTestCase {

  private static final SeedGenerator SEMIFAKE_SEED_GENERATOR
      = new SemiFakeSeedGenerator(new SplittableRandomAdapter());
  private DefaultSeedGenerator oldDefaultSeedGenerator;

  /**
   * The square root of 12, rounded from an extended-precision calculation that was done by Wolfram
   * Alpha (and thus at least as accurate as {@code StrictMath.sqrt(12.0)}).
   */
  protected static final double SQRT_12 = 3.4641016151377546;
  protected static final long TEST_SEED = 0x0123456789ABCDEFL;
  protected static final NamedFunction<Random, Double> NEXT_LONG =
      new NamedFunction<>(random -> (double) random.nextLong(), "Random::nextLong");
  protected static final NamedFunction<Random, Double> NEXT_INT =
      new NamedFunction<>(random -> (double) random.nextInt(), "Random::nextInt");
  protected static final NamedFunction<Random, Double> NEXT_DOUBLE =
      new NamedFunction<>(Random::nextDouble, "Random::nextDouble");
  protected static final NamedFunction<Random, Double> NEXT_GAUSSIAN =
      new NamedFunction<>(Random::nextGaussian, "Random::nextGaussian");
  protected static final NamedFunction<Random,Double> SET_SEED = new NamedFunction<>(random -> {
    if (random instanceof BaseRandom) {
      BaseRandom baseRandom = (BaseRandom) random;
      baseRandom.setSeed(SEMIFAKE_SEED_GENERATOR.generateSeed(baseRandom.getNewSeedLength()));
    } else {
      final ByteBuffer buffer = ByteBuffer.allocate(8);
      SEMIFAKE_SEED_GENERATOR.generateSeed(buffer.array());
      random.setSeed(buffer.getLong(0));
    }
    return 0.0;
  }, "BaseRandom::setSeed(byte[])");

  @SuppressWarnings("StaticCollection") protected static final List<NamedFunction<Random, Double>>
      FUNCTIONS_FOR_THREAD_SAFETY_TEST =
      ImmutableList.of(NEXT_LONG, NEXT_INT, NEXT_DOUBLE, NEXT_GAUSSIAN);
  @SuppressWarnings("StaticCollection") protected static final List<NamedFunction<Random, Double>>
      FUNCTIONS_FOR_THREAD_CRASH_TEST =
      ImmutableList.of(NEXT_LONG, NEXT_INT, NEXT_DOUBLE, NEXT_GAUSSIAN, SET_SEED);
  private static final int TEST_BYTE_ARRAY_LENGTH = 20;
  private static final String HELLO = "Hello";
  private static final String HOW_ARE_YOU = "How are you?";
  private static final String GOODBYE = "Goodbye";
  private static final String[] STRING_ARRAY = {HELLO, HOW_ARE_YOU, GOODBYE};
  @SuppressWarnings("StaticCollection") private static final List<String> STRING_LIST =
      Collections.unmodifiableList(Arrays.asList(STRING_ARRAY));
  private static final int ELEMENTS = 100;
  private static final double UPPER_BOUND_FOR_ROUNDING_TEST =
      Double.longBitsToDouble(Double.doubleToLongBits(1.0) + 3);
  protected final ForkJoinPool pool = new ForkJoinPool(2);

  @SafeVarargs
  private static <E> void testGeneratesAll(final Supplier<E> generator, final E... expected) {
    final E[] selected = Arrays.copyOf(expected, ELEMENTS); // Saves passing in a Class<E>
    for (int i = 0; i < ELEMENTS; i++) {
      selected[i] = generator.get();
    }
    assertTrue(Arrays.asList(selected).containsAll(Arrays.asList(expected)));
  }

  protected SeedGenerator getTestSeedGenerator() {
    return SEMIFAKE_SEED_GENERATOR;
  }

  protected EntropyCheckMode getEntropyCheckMode() {
    return EntropyCheckMode.EXACT;
  }

  @Test(timeOut = 120_000) public void testAllPublicConstructors()
      throws SeedException, IllegalAccessException, InstantiationException,
      InvocationTargetException {
    mockDefaultSeedGenerator();
    try {
      TestUtils.testConstructors(getClassUnderTest(), false, ImmutableMap.copyOf(constructorParams()),
          BaseRandom::nextInt);
    } finally {
      unmockDefaultSeedGenerator();
    }
  }

  protected Map<Class<?>, Object> constructorParams() {
    final int seedLength = getNewSeedLength(createRng());
    final HashMap<Class<?>, Object> params = new HashMap<>(4);
    params.put(int.class, seedLength);
    params.put(long.class, TEST_SEED);
    params.put(byte[].class, new byte[seedLength]);
    params.put(SeedGenerator.class, SEMIFAKE_SEED_GENERATOR);
    return params;
  }

  protected void mockDefaultSeedGenerator() {
    oldDefaultSeedGenerator = DefaultSeedGenerator.DEFAULT_SEED_GENERATOR;
    DefaultSeedGenerator mockDefaultSeedGenerator = PowerMockito.mock(DefaultSeedGenerator.class);
    when(mockDefaultSeedGenerator.generateSeed(anyInt())).thenAnswer(invocation ->
        SEMIFAKE_SEED_GENERATOR.generateSeed((Integer) (invocation.getArgument(0))));
    doAnswer(invocation -> {
      SEMIFAKE_SEED_GENERATOR.generateSeed((byte[]) invocation.getArgument(0));
      return null;
    }).when(mockDefaultSeedGenerator).generateSeed(any(byte[].class));
    Whitebox.setInternalState(DefaultSeedGenerator.class, "DEFAULT_SEED_GENERATOR",
        mockDefaultSeedGenerator);
  }

  protected void unmockDefaultSeedGenerator() {
    Whitebox.setInternalState(DefaultSeedGenerator.class, "DEFAULT_SEED_GENERATOR",
        oldDefaultSeedGenerator);
  }

  protected int getNewSeedLength(final BaseRandom basePrng) {
    return basePrng.getNewSeedLength();
  }

  protected abstract Class<? extends BaseRandom> getClassUnderTest();

  /**
   * Test to ensure that two distinct RNGs with the same seed return the same sequence of numbers.
   */
  @Test(timeOut = 15_000) public void testRepeatability() throws SeedException {
    final BaseRandom rng = createRng();
    // Create second RNG using same seed.
    final BaseRandom duplicateRNG = createRng(rng.getSeed());
    RandomTestUtils.assertEquivalent(rng, duplicateRNG, 100, "Output mismatch");
  }

  /**
   * Test that nextGaussian never returns a stale cached value.
   */
  @Test(timeOut = 15_000) public void testRepeatabilityNextGaussian() throws SeedException {
    final BaseRandom rng = createRng();
    byte[] seed = getTestSeedGenerator().generateSeed(getNewSeedLength(rng));
    rng.nextGaussian();
    rng.setSeed(seed);
    // Create second RNG using same seed.
    final BaseRandom duplicateRNG = createRng(seed);
    assertEquals(rng.nextGaussian(), duplicateRNG.nextGaussian());
  }

  @Test(timeOut = 15_000, expectedExceptions = IllegalArgumentException.class)
  public void testSeedTooLong() throws GeneralSecurityException, SeedException {
    createRng(getTestSeedGenerator()
        .generateSeed(getNewSeedLength(createRng()) + 1)); // Should throw an exception.
  }

  protected abstract BaseRandom createRng();

  protected abstract BaseRandom createRng(byte[] seed);

  /**
   * Test to ensure that the output from the RNG is broadly as expected.  This will not detect the
   * subtle statistical anomalies that would be picked up by Diehard, but it provides a simple check
   * for major problems with the output.
   */
  @Test(timeOut = 20_000, groups = "non-deterministic") public void testDistribution()
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
    for (final long n : new long[]{100, 1L << 32, Long.MAX_VALUE}) {
      final int iterations = 10_000;
      final SynchronizedDescriptiveStatistics stats =
          RandomTestUtils.summaryStats(rng, n, iterations);
      final double observedSD = stats.getStandardDeviation();
      final double expectedSD = n / SQRT_12;
      Reporter.log("Expected SD: " + expectedSD + ", observed SD: " + observedSD);
      assertGreaterOrEqual(observedSD, 0.97 * expectedSD);
      assertLessOrEqual(observedSD, 1.03 * expectedSD);
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
  @Test(timeOut = 40_000, groups = "non-deterministic") public void testNextGaussianStatistically()
      throws SeedException {
    final BaseRandom rng = createRng();
    final int iterations = 20_000;
    final SynchronizedDescriptiveStatistics stats = new SynchronizedDescriptiveStatistics();
    rng.gaussians(iterations).spliterator().forEachRemaining((DoubleConsumer) stats::addValue);
    final double observedSD = stats.getStandardDeviation();
    Reporter.log("Expected SD for Gaussians: 1, observed SD: " + observedSD);
    assertGreaterOrEqual(observedSD, 0.965);
    assertLessOrEqual(observedSD, 1.035);
    assertGreaterOrEqual(stats.getMax(), 2.0);
    assertLessOrEqual(stats.getMin(), -2.0);
    assertGreaterOrEqual(stats.getMean(), -0.1);
    assertLessOrEqual(stats.getMean(), 0.1);
    final double median = stats.getPercentile(50);
    assertGreaterOrEqual(median, -0.1);
    assertLessOrEqual(median, 0.1);
  }

  /**
   * Make sure that the RNG does not accept seeds that are too small since this could affect the
   * distribution of the output.
   */
  @Test(timeOut = 15_000, expectedExceptions = IllegalArgumentException.class)
  public void testSeedTooShort() throws SeedException {
    createRng(new byte[]{1, 2, 3}); // One byte too few, should cause an IllegalArgumentException.
  }

  /**
   * RNG must not accept a null seed otherwise it will not be properly initialised.
   */
  @Test(timeOut = 15_000, expectedExceptions = IllegalArgumentException.class)
  public void testNullSeed() throws SeedException {
    createRng(null);
  }

  @Test(timeOut = 15_000) public void testSerializable()
      throws IOException, ClassNotFoundException, SeedException {
    // Serialise an RNG.
    final BaseRandom rng = createRng();
    RandomTestUtils.assertEquivalentWhenSerializedAndDeserialized(rng);
    // Can't use a SemiFakeSeedGenerator, because Random.equals() breaks equality check
    final SeedGenerator seedGenerator = new FakeSeedGenerator(
        getClass().getSimpleName() + "::testSerializable");
    rng.setSeedGenerator(seedGenerator);
    try {
      final BaseRandom rng2 = CloneViaSerialization.clone(rng);
      try {
        assertEquals(seedGenerator, rng2.getSeedGenerator());
      } finally {
        rng2.setSeedGenerator(null);
      }
    } finally {
      rng.setSeedGenerator(null);
      RandomSeederThread.stopIfEmpty(seedGenerator);
    }
  }

  /** Assertion-free since many implementations have a fallback behavior. */
  @Test(timeOut = 60_000) public void testSetSeedLong() {
    mockDefaultSeedGenerator();
    try {
      createRng().setSeed(0x0123456789ABCDEFL);
    } finally {
      unmockDefaultSeedGenerator();
    }
  }

  @Test(timeOut = 15_000) public void testSetSeedAfterNextLong() throws SeedException {
    final byte[] seed =
        getTestSeedGenerator().generateSeed(getNewSeedLength(createRng()));
    final BaseRandom rng = createRng();
    final BaseRandom rng2 = createRng();
    final BaseRandom rng3 = createRng(seed);
    rng.nextLong(); // ensure rng & rng2 won't both be in initial state before reseeding
    rng.setSeed(seed);
    rng2.setSeed(seed);
    RandomTestUtils.assertEquivalent(rng, rng2, 64,
        "Output mismatch after reseeding with same seed");
    rng.setSeed(seed);
    RandomTestUtils.assertEquivalent(rng, rng3, 64,
        "Output mismatch vs a new PRNG with same seed");
  }

  @Test(timeOut = 15_000) public void testSetSeedAfterNextInt() throws SeedException {
    final byte[] seed =
        getTestSeedGenerator().generateSeed(getNewSeedLength(createRng()));
    final BaseRandom rng = createRng();
    final BaseRandom rng2 = createRng();
    final BaseRandom rng3 = createRng(seed);
    rng.nextInt(); // ensure rng & rng2 won't both be in initial state before reseeding
    rng.setSeed(seed);
    rng2.setSeed(seed);
    RandomTestUtils.assertEquivalent(rng, rng2, 64,
        "Output mismatch after reseeding with same seed");
    rng.setSeed(seed);
    RandomTestUtils.assertEquivalent(rng, rng3, 64,
        "Output mismatch vs a new PRNG with same seed");
  }

  @Test(timeOut = 15_000) public void testSetSeedZero() throws SeedException {
    int length = getNewSeedLength(createRng());
    final byte[] zeroSeed = new byte[length];
    final byte[] realSeed = new byte[length];
    do {
      getTestSeedGenerator().generateSeed(realSeed);
    } while (Arrays.equals(realSeed, zeroSeed));
    final BaseRandom rng = createRng(realSeed);
    final BaseRandom rng2 = createRng(zeroSeed);
    RandomTestUtils.assertDistinct(rng, rng2, 20,
        "Output with real seed matches output with all-zeroes seed");
  }

  @Test(timeOut = 15_000) public void testEquals() throws SeedException {
    RandomTestUtils.doEqualsSanityChecks(this::createRng);
  }

  @Test(timeOut = 60_000) public void testHashCode() throws Exception {
    assert RandomTestUtils.testHashCodeDistribution(this::createRng)
        : "Too many hashCode collisions";
  }

  /**
   * dump() doesn't have much of a contract, but we should at least expect it to output enough state
   * for two independently-generated instances to give unequal dumps.
   */
  @Test(timeOut = 15_000) public void testDump() throws SeedException {
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
    rng1.setSeed(getTestSeedGenerator().generateSeed(seedLength));
    assertGreaterOrEqual(rng1.getEntropyBits(), seedLength * 8L);
    rng1.nextBytes(output1);
    rng2.nextBytes(output2);
    assertFalse(Arrays.equals(output1, output2));
  }

  /**
   * This also tests {@link BaseRandom#getSeedGenerator()} and
   * {@link BaseRandom#setSeedGenerator(SeedGenerator)}.
   *
   * @throws Exception
   */
  @SuppressWarnings("BusyWait") @Test(timeOut = 60_000)
  public void testRandomSeederThreadIntegration() throws Exception {
    final SeedGenerator seedGenerator = new SemiFakeSeedGenerator(new Random());
    final BaseRandom rng = createRng();
    final byte[] oldSeed = rng.getSeed();
    while (rng.getEntropyBits() > Long.SIZE) {
      rng.nextLong();
    }
    RandomSeederThread.setPriority(seedGenerator, Thread.MAX_PRIORITY);
    rng.setSeedGenerator(seedGenerator);
    try {
      int waits = 0;
      byte[] newSeed;
      do {
        assertSame(rng.getSeedGenerator(), seedGenerator);
        rng.nextBoolean();
        Thread.sleep(10);
        waits++;
        newSeed = rng.getSeed();
      } while (Arrays.equals(newSeed, oldSeed) && (waits < 1000));
      if (waits >= 1000) {
        fail(String.format("Timed out waiting for %s to be reseeded!", rng));
      }
      Thread.sleep(100); // entropy update may not be co-atomic with seed update
      assertGreaterOrEqual(rng.getEntropyBits(), (newSeed.length * 8L) - 1);
    } finally {
      rng.setSeedGenerator(null);
      RandomSeederThread.stopIfEmpty(seedGenerator);
    }
    assertNull(rng.getSeedGenerator());
  }

  @Test(timeOut = 10_000) public void testWithProbability() {
    final BaseRandom prng = createRng();
    final long originalEntropy = prng.getEntropyBits();
    assertFalse(prng.withProbability(0.0));
    assertTrue(prng.withProbability(1.0));
    assertEquals(originalEntropy, prng.getEntropyBits());
    checkRangeAndEntropy(prng, 1, () -> prng.withProbability(0.7) ? 0 : 1, 0, 2,
        getEntropyCheckMode());
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
    trues = 0;
    for (int i = 0; i < 3000; i++) {
      if (prng.withProbability(0.5)) {
        trues++;
      }
    }
    // Significance test at p=4.54E-6 (unadjusted for the multiple subclasses and environments!)
    assertGreaterOrEqual(trues, 1375);
    assertLessOrEqual(trues, 1625);
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
    final long entropy = prng.getEntropyBits();
    final long expectedEntropy = oldEntropy - (8 * TEST_BYTE_ARRAY_LENGTH);
    switch (getEntropyCheckMode()) {
      case EXACT:
        assertEquals(entropy, expectedEntropy);
        break;
      case LOWER_BOUND:
        assertGreaterOrEqual(entropy, expectedEntropy);
        break;
      case OFF:
        break;
    }
  }

  @Test public void testNextInt1() throws Exception {
    final BaseRandom prng = createRng();
    final Supplier<? extends Number> numberSupplier = () -> prng.nextInt(3 << 29);
    checkRangeAndEntropy(prng, 31, numberSupplier, 0, (3 << 29), getEntropyCheckMode());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNextInt1InvalidBound() {
    createRng().nextInt(0);
  }

  @Test public void testNextInt() throws Exception {
    final BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, 32, (Supplier<? extends Number>) prng::nextInt, Integer.MIN_VALUE,
        (Integer.MAX_VALUE + 1L), getEntropyCheckMode());
  }

  @Test public void testNextInt2() throws Exception {
    final BaseRandom prng = createRng();
    final Supplier<? extends Number> numberSupplier = () -> prng.nextInt(1 << 27, 1 << 29);
    checkRangeAndEntropy(prng, 29, numberSupplier, (1 << 27), (1 << 29), getEntropyCheckMode());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNextInt2InvalidBound() {
    createRng().nextInt(1, 1);
  }

  @Test public void testNextInt2HugeRange() throws Exception {
    final BaseRandom prng = createRng();
    final Supplier<? extends Number> numberSupplier =
        () -> prng.nextInt(Integer.MIN_VALUE, 1 << 29);
    checkRangeAndEntropy(prng, 32, numberSupplier, Integer.MIN_VALUE, (1 << 29),
        getEntropyCheckMode());
  }

  @Test public void testNextLong() throws Exception {
    final BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, 64, (Supplier<? extends Number>) prng::nextLong, Long.MIN_VALUE,
        Long.MAX_VALUE + 1.0, getEntropyCheckMode());
  }

  @Test public void testNextLong1() throws Exception {
    final BaseRandom prng = createRng();
    for (int i = 0; i < 20; i++) {
      // check that the bound is exclusive, to kill an off-by-one mutant
      final Supplier<? extends Number> numberSupplier = () -> prng.nextLong(2);
      checkRangeAndEntropy(prng, 1, numberSupplier, 0, 2, getEntropyCheckMode());
    }
    final Supplier<? extends Number> numberSupplier = () -> prng.nextLong(1L << 42);
    checkRangeAndEntropy(prng, 42, numberSupplier, 0, (1L << 42), getEntropyCheckMode());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNextLong1InvalidBound() {
    createRng().nextLong(-1);
  }

  @Test public void testNextLong2() throws Exception {
    final BaseRandom prng = createRng();
    final Supplier<? extends Number> numberSupplier = () -> prng.nextLong(1L << 40, 1L << 42);
    checkRangeAndEntropy(prng, 42, numberSupplier, (1L << 40), (1L << 42), getEntropyCheckMode());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNextLong2InvalidBound() {
    createRng().nextLong(10, 9);
  }

  @Test public void testNextLong2HugeRange() throws Exception {
    final BaseRandom prng = createRng();
    final Supplier<? extends Number> numberSupplier = () -> prng.nextLong(Long.MIN_VALUE, 1L << 62);
    checkRangeAndEntropy(prng, 64, numberSupplier, Long.MIN_VALUE, (1L << 62),
        getEntropyCheckMode());
  }

  @Test public void testNextDouble() throws Exception {
    final BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, ENTROPY_OF_DOUBLE, (Supplier<? extends Number>) prng::nextDouble,
        0.0, 1.0, getEntropyCheckMode());
  }

  @Test public void testNextFloat() throws Exception {
    final BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, ENTROPY_OF_FLOAT, (Supplier<? extends Number>) prng::nextFloat, 0.0,
        1.0, getEntropyCheckMode());
  }

  @Test public void testNextDouble1() throws Exception {
    final BaseRandom prng = createRng();
    final Supplier<? extends Number> numberSupplier = () -> prng.nextDouble(13.37);
    checkRangeAndEntropy(prng, ENTROPY_OF_DOUBLE, numberSupplier, 0.0, 13.37,
        getEntropyCheckMode());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNextDouble1InvalidBound() {
    createRng().nextDouble(-1.0);
  }

  @Test public void testNextDouble2() throws Exception {
    final BaseRandom prng = createRng();
    final Supplier<? extends Number> numberSupplier2 = () -> prng.nextDouble(-1.0, 13.37);
    checkRangeAndEntropy(prng, ENTROPY_OF_DOUBLE, numberSupplier2, -1.0, 13.37,
        getEntropyCheckMode());
    final Supplier<? extends Number> numberSupplier1 = () -> prng.nextDouble(5.0, 13.37);
    checkRangeAndEntropy(prng, ENTROPY_OF_DOUBLE, numberSupplier1, 5.0, 13.37,
        getEntropyCheckMode());
    final Supplier<? extends Number> numberSupplier =
        () -> prng.nextDouble(1.0, UPPER_BOUND_FOR_ROUNDING_TEST);
    checkRangeAndEntropy(prng, ENTROPY_OF_DOUBLE, numberSupplier, 1.0,
        UPPER_BOUND_FOR_ROUNDING_TEST, getEntropyCheckMode());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNextDouble2InvalidBound() {
    createRng().nextDouble(3.5, 3.5);
  }

  @Test public void testNextGaussian() throws Exception {
    final BaseRandom prng = createRng();
    // TODO: Find out the actual Shannon entropy of nextGaussian() and adjust the entropy count to
    // it in a wrapper function.
    checkRangeAndEntropy(prng, 2 * ENTROPY_OF_DOUBLE,
        () -> prng.nextGaussian() + prng.nextGaussian(), -Double.MAX_VALUE, Double.MAX_VALUE,
        getEntropyCheckMode());
  }

  @Test public void testNextBoolean() throws Exception {
    final BaseRandom prng = createRng();
    final Supplier<? extends Number> numberSupplier = () -> prng.nextBoolean() ? 0 : 1;
    checkRangeAndEntropy(prng, 1L, numberSupplier, 0, 2, getEntropyCheckMode());
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

  @Test(timeOut = 10_000) public void testLongs3SmallRange() throws Exception {
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
    testGeneratesAll(() -> prng.nextElement(STRING_ARRAY), STRING_ARRAY);
  }

  @Test public void testNextElementList() {
    final BaseRandom prng = createRng();
    testGeneratesAll(() -> prng.nextElement(STRING_LIST), STRING_ARRAY);
  }

  @Test public void testNextEnum() {
    final BaseRandom prng = createRng();
    testGeneratesAll(() -> prng.nextEnum(TestEnum.class), TestEnum.RED, TestEnum.YELLOW,
        TestEnum.BLUE);
  }

  @Test public void testGetNewSeedLength() {
    assertTrue(createRng().getNewSeedLength() > 0);
  }

  @Test(timeOut = 90_000) public void testThreadSafety() {
    testThreadSafety(FUNCTIONS_FOR_THREAD_SAFETY_TEST, FUNCTIONS_FOR_THREAD_SAFETY_TEST);
  }

  @Test(timeOut = 90_000) public void testThreadSafetySetSeed() {
    testThreadSafetyVsCrashesOnly(30, FUNCTIONS_FOR_THREAD_CRASH_TEST);
  }

  protected void testThreadSafetyVsCrashesOnly(final int timeoutSec,
      final List<NamedFunction<Random, Double>> functions) {
    final int seedLength = createRng().getNewSeedLength();
    final byte[] seed = getTestSeedGenerator().generateSeed(seedLength);
    for (final NamedFunction<Random, Double> supplier1 : functions) {
      for (final NamedFunction<Random, Double> supplier2 : functions) {
        runParallel(supplier1, supplier2, seed, timeoutSec,
            (supplier1 == SET_SEED || supplier2 == SET_SEED) ? 200 : 1000);
      }
    }
  }

  @SuppressWarnings({"EqualityOperatorComparesObjects", "ObjectEquality"})
  protected void testThreadSafety(final List<NamedFunction<Random, Double>> functions,
      final List<NamedFunction<Random, Double>> pairwiseFunctions) {
    final int seedLength = createRng().getNewSeedLength();
    final byte[] seed = getTestSeedGenerator().generateSeed(seedLength);
    for (final NamedFunction<Random, Double> supplier : functions) {
      for (int i = 0; i < 5; i++) {
        // This loop is necessary to control the false pass rate, especially during mutation testing.
        SortedSet<Double> sequentialOutput = runSequential(supplier, supplier, seed);
        SortedSet<Double> parallelOutput = runParallel(supplier, supplier, seed, 10, 1000);
        assertEquals(sequentialOutput, parallelOutput,
            "output differs between sequential & parallel calls to " + supplier);
      }
    }

    // Check that each pair won't crash no matter which order they start in
    // (this part is assertion-free because we can't tell whether A-bits-as-long and
    // B-bits-as-double come from the same bit stream as vice-versa).
    for (final NamedFunction<Random, Double> supplier1 : pairwiseFunctions) {
      for (final NamedFunction<Random, Double> supplier2 : pairwiseFunctions) {
        if (supplier1 != supplier2) {
          runParallel(supplier1, supplier2, seed, 10, 1000);
        }
      }
    }
  }

  protected SortedSet<Double> runParallel(final NamedFunction<Random, Double> supplier1,
      final NamedFunction<Random, Double> supplier2, final byte[] seed, int timeoutSec,
      int iterations) {
    // See https://www.yegor256.com/2018/03/27/how-to-test-thread-safety.html for why a
    // CountDownLatch is used.
    final CountDownLatch latch = new CountDownLatch(2);
    final Random parallelPrng = createRng(seed);
    final SortedSet<Double> output = new ConcurrentSkipListSet<>();
    pool.execute(new GeneratorForkJoinTask(parallelPrng, output, supplier1, latch, iterations));
    pool.execute(new GeneratorForkJoinTask(parallelPrng, output, supplier2, latch, iterations));
    assertTrue(pool.awaitQuiescence(timeoutSec, TimeUnit.SECONDS),
        String.format("Timed out waiting for %s and %s to finish", supplier1, supplier2));
    return output;
  }

  protected SortedSet<Double> runSequential(final NamedFunction<Random, Double> supplier1,
      final NamedFunction<Random, Double> supplier2, final byte[] seed) {
    final Random sequentialPrng = createRng(seed);
    final SortedSet<Double> output = new TreeSet<>();
    new GeneratorForkJoinTask(sequentialPrng, output, supplier1, new CountDownLatch(1),
        1000)
        .exec();
    new GeneratorForkJoinTask(sequentialPrng, output, supplier2, new CountDownLatch(1),
        1000)
        .exec();
    return output;
  }

  @AfterClass public void classTearDown() {
    RandomSeederThread.stopIfEmpty(getTestSeedGenerator());
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

    private static final long serialVersionUID = 9155874155769888368L;
    private final Random prng;
    private final SortedSet<T> set;
    private final NamedFunction<Random, T> function;
    private final CountDownLatch latch;
    private final int iterations;

    public GeneratorForkJoinTask(final Random prng, final SortedSet<T> set,
        final NamedFunction<Random, T> function, CountDownLatch latch, int iterations) {
      this.prng = prng;
      this.set = set;
      this.function = function;
      this.latch = latch;
      this.iterations = iterations;
    }

    @Override public Void getRawResult() {
      return null;
    }

    @Override protected void setRawResult(final Void value) {
      // No-op.
    }

    @Override protected boolean exec() {
      latch.countDown();
      try {
        latch.await();
      } catch (InterruptedException e) {
        throw new AssertionError("Interrupted", e);
      }
      for (int i = 0; i < iterations; i++) {
        set.add(function.apply(prng));
      }
      return true;
    }
  }

  protected static final class NamedFunction<T, R> implements Function<T, R>, Serializable {

    private final Function<T, R> function;
    private final String name;

    public NamedFunction(final Function<T, R> function, final String name) {
      this.function = function;
      this.name = name;
    }

    @Override public R apply(final T t) {
      return function.apply(t);
    }

    @Override public String toString() {
      return name;
    }
  }
}
