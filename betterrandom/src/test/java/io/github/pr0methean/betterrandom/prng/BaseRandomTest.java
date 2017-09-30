package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.prng.BaseRandom.ENTROPY_OF_DOUBLE;
import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.assertMonteCarloPiEstimateSane;
import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.checkRangeAndEntropy;
import static io.github.pr0methean.betterrandom.prng.RandomTestUtils.checkStream;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import io.github.pr0methean.betterrandom.TestingDeficiency;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils.EntropyCheckMode;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import io.github.pr0methean.betterrandom.util.LogPreFormatter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.Test;
import org.testng.util.RetryAnalyzerCount;

public abstract class BaseRandomTest {

  private enum TestEnum {
    RED,
    YELLOW,
    BLUE;
  }

  ;

  /**
   * The square root of 12, rounded from an extended-precision calculation that was done by Wolfram
   * Alpha (and thus at least as accurate as {@code StrictMath.sqrt(12.0)}).
   */
  protected static final double SQRT_12 = 3.4641016151377546;

  private static final LogPreFormatter LOG = new LogPreFormatter(BaseRandomTest.class);
  private static final int MAX_DUMPED_SEED_LENGTH = 32;
  private static final int FLAKY_TEST_RETRIES = 3;
  private static final int TEST_BYTE_ARRAY_LENGTH = 20;

  private static final String HELLO = "Hello";
  private static final String HOW_ARE_YOU = "How are you?";
  private static final String GOODBYE = "Goodbye";
  private static final String[] STRING_ARRAY = {HELLO, HOW_ARE_YOU, GOODBYE};
  private static final List<String> STRING_LIST = Arrays.asList(STRING_ARRAY);

  /**
   * Test to ensure that two distinct RNGs with the same seed return the same sequence of numbers.
   */
  @Test(timeOut = 15000)
  public void testRepeatability() throws SeedException {
    final BaseRandom rng = createRng();
    final byte[] seed = rng.getSeed();
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
        String.format("Generated sequences do not match between:%n%s%nand:%n%s", rng.dump(),
            duplicateRNG.dump());
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
  @Test(timeOut = 20000, groups = "non-deterministic", retryAnalyzer = FlakyTestRetrier.class)
  public void testDistribution() throws SeedException {
    final BaseRandom rng = createRng();
    assertMonteCarloPiEstimateSane(rng);
  }

  /**
   * Test to ensure that the output from the RNG is broadly as expected.  This will not detect the
   * subtle statistical anomalies that would be picked up by Diehard, but it provides a simple check
   * for major problems with the output.
   */
  @Test(timeOut = 20000, groups = "non-deterministic", retryAnalyzer = FlakyTestRetrier.class)
  public void testStandardDeviation() throws SeedException {
    final BaseRandom rng = createRng();
    // Expected standard deviation for a uniformly distributed population of values in the range 0..n
    // approaches n/sqrt(12).
    // Expected standard deviation for a uniformly distributed population of values in the range 0..n
    // approaches n/sqrt(12).
    final int n = 100;
    final double observedSD = RandomTestUtils
        .calculateSampleStandardDeviation((Random) rng, n, 10000);
    final double expectedSD = n / SQRT_12;
    Reporter.log("Expected SD: " + expectedSD + ", observed SD: " + observedSD);
    assertEquals(observedSD, expectedSD, 0.02 * expectedSD,
        "Standard deviation is outside acceptable range: " + observedSD);
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

  @Test(timeOut = 20000, retryAnalyzer = FlakyTestRetrier.class)
  public void testReseeding() throws Exception {
    final BaseRandom rng = createRng();
    rng.setSeederThread(RandomTestUtils.DEFAULT_SEEDER);
    final byte[] oldSeed = rng.getSeed();
    rng.nextBytes(new byte[20000]);
    Thread.sleep(5000);
    final byte[] newSeed = rng.getSeed();
    assertFalse(Arrays.equals(oldSeed, newSeed));
    rng.setSeederThread(null);
  }

  @Test(timeOut = 3000)
  public void testWithProbability() {
    final BaseRandom prng = createRng();
    final long originalEntropy = prng.getEntropyBits();
    assertFalse(prng.withProbability(0.0));
    assertTrue(prng.withProbability(1.0));
    assertEquals(originalEntropy, prng.getEntropyBits());
    checkRangeAndEntropy(prng, 1, () -> prng.withProbability(0.7) ? 0 : 1, 0, 2,
        true);
  }

  @Test
  public void testNextBytes() throws Exception {
    byte[] testBytes = new byte[TEST_BYTE_ARRAY_LENGTH];
    BaseRandom prng = createRng();
    long oldEntropy = prng.getEntropyBits();
    prng.nextBytes(testBytes);
    assertFalse(Arrays.equals(testBytes, new byte[TEST_BYTE_ARRAY_LENGTH]));
    assertEquals(prng.getEntropyBits(), oldEntropy - 8 * TEST_BYTE_ARRAY_LENGTH);
  }

  @Test
  public void testNextInt1() throws Exception {
    final BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, 31,
        () -> prng.nextInt(3 << 29), 0, 3 << 29, true);
  }

  @Test
  public void testNextInt2() throws Exception {
    final BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, 32,
        prng::nextInt, Integer.MIN_VALUE, Integer.MAX_VALUE + 1L, true);
  }

  @Test
  public void testNextLong() throws Exception {
    final BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, 64, prng::nextLong, Long.MIN_VALUE,
        Long.MAX_VALUE + 1.0, true);
  }

  @Test
  public void testNextDouble() throws Exception {
    final BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, ENTROPY_OF_DOUBLE, prng::nextDouble, 0.0, 1.0, true);
  }

  @Test
  public void testNextGaussian() throws Exception {
    final BaseRandom prng = createRng();
    // TODO: Find out the actual Shannon entropy of nextGaussian() and adjust the entropy count to
    // it in a wrapper function.
    checkRangeAndEntropy(prng, 2 * ENTROPY_OF_DOUBLE,
        () -> prng.nextGaussian() + prng.nextGaussian(), -Double.MAX_VALUE, Double.MAX_VALUE,
        EntropyCheckMode.EXACT);
  }

  @Test
  public void testNextBoolean() throws Exception {
    final BaseRandom prng = createRng();
    checkRangeAndEntropy(prng, 1, () -> prng.nextBoolean() ? 0 : 1, 0, 2, true);
  }

  @Test
  public void testInts() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, 32, prng.ints(), -1, Integer.MIN_VALUE, Integer.MAX_VALUE + 1L,
        true);
  }

  @Test
  public void testInts1() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, 32, prng.ints(20), 20, Integer.MIN_VALUE, Integer.MAX_VALUE + 1L,
        true);
  }

  @Test
  public void testInts2() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, 29, prng.ints(1 << 27, 1 << 29), -1, 1 << 27, 1 << 29,
        true);
  }

  @Test
  public void testInts3() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, 29, prng.ints(3, 1 << 27, 1 << 29), 3, 1 << 27, 1 << 29,
        true);
  }

  @Test
  public void testLongs() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, 64, prng.longs(), -1, Long.MIN_VALUE, Long.MAX_VALUE + 1.0,
        true);
  }

  @Test
  public void testLongs1() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, 64, prng.longs(20), 20, Long.MIN_VALUE, Long.MAX_VALUE + 1.0,
        true);
  }

  @Test
  public void testLongs2() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, 42, prng.longs(1L << 40, 1L << 42), -1, 1L << 40, 1L << 42,
        true);
  }

  @Test
  public void testLongs3() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, 42, prng.longs(20, 1L << 40, 1L << 42), 20, 1L << 40, 1L << 42,
        true);
  }

  @Test
  public void testDoubles() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, ENTROPY_OF_DOUBLE, prng.doubles(), -1, 0.0, 1.0, true);
  }

  @Test
  public void testDoubles1() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, ENTROPY_OF_DOUBLE, prng.doubles(20), 20, 0.0, 1.0, true);
  }

  @Test
  public void testDoubles2() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, ENTROPY_OF_DOUBLE, prng.doubles(-5.0, 8.0), -1, -5.0, 8.0,
        true);
  }

  @Test
  public void testDoubles3() throws Exception {
    final BaseRandom prng = createRng();
    checkStream(prng, ENTROPY_OF_DOUBLE, prng.doubles(20, -5.0, 8.0), 20, -5.0, 8.0,
        true);
  }

  private static final int ELEMENTS = 100;

  private <E> void testGeneratesAll(Supplier<E> generator, E... expected) {
    final BaseRandom prng = createRng();
    final E[] selected = Arrays.copyOf(expected, ELEMENTS); // Saves passing in a Class<E>
    for (int i = 0; i < ELEMENTS; i++) {
      selected[i] = generator.get();
    }
    assertTrue(Arrays.asList(selected).containsAll(Arrays.asList(expected)));
  }

  @Test
  public void testNextElementArray() {
    final BaseRandom prng = createRng();
    testGeneratesAll(() -> prng.nextElement(STRING_ARRAY), STRING_ARRAY);
  }

  @Test
  public void testNextElementList() {
    final BaseRandom prng = createRng();
    testGeneratesAll(() -> prng.nextElement(STRING_LIST), STRING_ARRAY);
  }

  @Test
  public void testNextEnum() {
    final BaseRandom prng = createRng();
    testGeneratesAll(() -> prng.nextEnum(TestEnum.class), TestEnum.RED, TestEnum.YELLOW,
        TestEnum.BLUE);
  }

  private static final class FlakyTestRetrier extends RetryAnalyzerCount {

    @EntryPoint
    public FlakyTestRetrier() {
      setCount(FLAKY_TEST_RETRIES);
    }

    @Override
    public boolean retryMethod(final ITestResult iTestResult) {
      return true;
    }
  }
}
