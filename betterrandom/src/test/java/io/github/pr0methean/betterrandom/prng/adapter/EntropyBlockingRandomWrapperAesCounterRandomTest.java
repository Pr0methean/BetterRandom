package io.github.pr0methean.betterrandom.prng.adapter;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import com.google.common.collect.ImmutableMap;
import com.google.common.testing.SerializableTester;
import io.github.pr0methean.betterrandom.DeadlockWatchdog;
import io.github.pr0methean.betterrandom.FlakyRetryAnalyzer;
import io.github.pr0methean.betterrandom.TestUtils;
import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import io.github.pr0methean.betterrandom.seed.FakeSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeeder;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SemiFakeSeedGenerator;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import org.testng.annotations.Test;

// FIXME: Some tests are sometimes too slow.
@Test(testName = "EntropyBlockingRandomWrapper:AesCounterRandom")
public class EntropyBlockingRandomWrapperAesCounterRandomTest extends RandomWrapperAesCounterRandomTest {
  private static final long DEFAULT_MAX_ENTROPY = -64L;
  protected static final long VERY_LOW_MINIMUM_ENTROPY = Long.MIN_VALUE / 2;

  @Override @Test(timeOut = 60_000) public void testDistribution() throws SeedException {
    super.testDistribution();
  }

  @Override @Test(timeOut = 60_000) public void testDoubles1() {
    super.testDoubles1();
  }

  @Override @Test(timeOut = 60_000) public void testDoubles2() {
    super.testDoubles2();
  }

  @Override @Test(timeOut = 60_000) public void testDoubles3() {
    super.testDoubles3();
  }

  @Override @Test(timeOut = 60_000) public void testDoubles3RoundingCorrection() {
    super.testDoubles3RoundingCorrection();
  }

  @Override @Test(timeOut = 60_000) public void testDump() throws SeedException {
    DeadlockWatchdog.ensureStarted();
    try {
      super.testDump();
    } finally {
      DeadlockWatchdog.stopInstance();
    }
  }

  /**
   * Assertion-free since reseeding may cause divergent output.
   */
  @Override @Test(timeOut = 10_000) public void testSetSeedLong() {
    createRng().setSeed(0x0123456789ABCDEFL);
  }

  @Override @Test public void testAllPublicConstructors() {
    Constructor<?>[] constructors =
        EntropyBlockingRandomWrapper.class.getDeclaredConstructors();
    ArrayList<Constructor<?>> relevantConstructors = new ArrayList<>(constructors.length);
    for (Constructor<?> constructor : constructors) {
      if (Arrays.asList(constructor.getParameterTypes()).contains(Random.class)) {
        relevantConstructors.add(constructor);
      }
    }
    TestUtils.testConstructors(false, ImmutableMap.copyOf(constructorParams()),
        (Consumer<? super EntropyBlockingRandomWrapper>) BaseRandom::nextInt,
        relevantConstructors);
  }

  @Override public Class<? extends BaseRandom> getClassUnderTest() {
    return EntropyBlockingRandomWrapper.class;
  }

  @Override protected RandomWrapper createRng() throws SeedException {
    SeedGenerator testSeedGenerator = getTestSeedGenerator();
    return new EntropyBlockingRandomWrapper(new AesCounterRandom(testSeedGenerator), DEFAULT_MAX_ENTROPY,
        testSeedGenerator);
  }

  @Override protected RandomWrapper createRng(byte[] seed) throws SeedException {
    SeedGenerator testSeedGenerator = getTestSeedGenerator();
    return new EntropyBlockingRandomWrapper(new AesCounterRandom(seed), DEFAULT_MAX_ENTROPY,
        testSeedGenerator);
  }

  @Override public Map<Class<?>, Object> constructorParams() {
    Map<Class<?>, Object> out = super.constructorParams();
    out.put(long.class, DEFAULT_MAX_ENTROPY);
    out.put(Random.class, new AesCounterRandom(getTestSeedGenerator()));
    return out;
  }

  @Override public void testThreadSafety() {
    checkThreadSafety(functionsForThreadSafetyTest, functionsForThreadSafetyTest,
        this::createRngLargeEntropyLimit);
  }

  private EntropyBlockingRandomWrapper createRngLargeEntropyLimit() {
    final SeedGenerator testSeedGenerator = getTestSeedGenerator();
    return new EntropyBlockingRandomWrapper(new AesCounterRandom(testSeedGenerator),
        VERY_LOW_MINIMUM_ENTROPY, testSeedGenerator);
  }

  private BaseRandom createRngLargeEntropyLimit(byte[] seed) {
    return new EntropyBlockingRandomWrapper(new AesCounterRandom(seed),
        VERY_LOW_MINIMUM_ENTROPY, getTestSeedGenerator());
  }

  @Override public void testRepeatability() throws SeedException {
    SeedGenerator testSeedGenerator = getTestSeedGenerator();
    final BaseRandom rng = new EntropyBlockingRandomWrapper(new AesCounterRandom(testSeedGenerator),
        VERY_LOW_MINIMUM_ENTROPY, testSeedGenerator);
    // Create second RNG using same seed.
    final BaseRandom duplicateRNG = createRngLargeEntropyLimit(rng.getSeed());
    RandomTestUtils.assertEquivalent(rng, duplicateRNG, TEST_BYTES_LENGTH, "Output mismatch");
  }

  @Override protected RandomTestUtils.EntropyCheckMode getEntropyCheckMode() {
    return RandomTestUtils.EntropyCheckMode.LOWER_BOUND;
  }

  @Override public void testSerializable() throws SeedException {
    // Can't use a SemiFakeSeedGenerator, because Random.equals() breaks equality check
    final SeedGenerator seedGenerator =
        new FakeSeedGenerator(getClass().getSimpleName() + "::testSerializable #" + new Random().nextInt());
    // Serialise an RNG.
    final BaseRandom rng = new EntropyBlockingRandomWrapper(new AesCounterRandom(seedGenerator),
        VERY_LOW_MINIMUM_ENTROPY,
        seedGenerator);
    RandomTestUtils.assertEquivalentWhenSerializedAndDeserialized(rng);
    RandomSeeder randomSeeder = new RandomSeeder(seedGenerator);
    rng.setRandomSeeder(randomSeeder);
    try {
      final BaseRandom rng2 = SerializableTester.reserialize(rng);
      assertEquals(randomSeeder, rng2.getRandomSeeder());
      rng2.setRandomSeeder(null);
    } finally {
      RandomTestUtils.removeAndAssertEmpty(randomSeeder, rng);
    }
  }

  @Override public void testNextBytes() {
    final byte[] testBytes = new byte[TEST_BYTE_ARRAY_LENGTH];
    final BaseRandom prng = new EntropyBlockingRandomWrapper(0L, getTestSeedGenerator());
    final long oldEntropy = prng.getEntropyBits();
    prng.nextBytes(testBytes);
    assertFalse(Arrays.equals(testBytes, new byte[TEST_BYTE_ARRAY_LENGTH]));
  }

  // FIXME: Too slow
  @Override @Test(timeOut = 60_000L) public void testDoubles() {
    super.testDoubles();
  }

  // FIXME: Too slow, and why is such a huge entropy adjustment needed?!
  @Override @Test(timeOut = 90_000L, retryAnalyzer = FlakyRetryAnalyzer.class)
  public void testRandomSeederIntegration() {
    final SeedGenerator seedGenerator = new SemiFakeSeedGenerator(new Random(),
        UUID.randomUUID().toString());
    final BaseRandom rng = createRng();
    RandomTestUtils.checkReseeding(seedGenerator, rng, true, 1 << 30);
  }
}
