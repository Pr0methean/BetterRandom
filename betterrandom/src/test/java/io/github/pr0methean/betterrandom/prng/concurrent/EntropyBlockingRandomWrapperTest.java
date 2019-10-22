package io.github.pr0methean.betterrandom.prng.concurrent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

import com.google.common.collect.ImmutableList;
import io.github.pr0methean.betterrandom.FlakyRetryAnalyzer;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import io.github.pr0methean.betterrandom.seed.FailingSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SemiFakeSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeederThread.DefaultThreadFactory;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadFactory;
import org.mockito.Mockito;
import org.testng.annotations.Test;

@Test(testName = "EntropyBlockingRandomWrapper")
public class EntropyBlockingRandomWrapperTest extends RandomWrapperRandomTest {

  @Override public Class<? extends BaseRandom> getClassUnderTest() {
    return EntropyBlockingRandomWrapper.class;
  }

  @Override public Map<Class<?>, Object> constructorParams() {
    Map<Class<?>, Object> out = super.constructorParams();
    out.put(long.class, EntropyBlockingTestUtils.DEFAULT_MAX_ENTROPY);
    return out;
  }

  @Override protected SeedGenerator getTestSeedGenerator() {
    // Need a separate and non-value-equal instance for each test, for isolation
    return new SemiFakeSeedGenerator(
        new SplittableRandomAdapter(SecureRandomSeedGenerator.DEFAULT_INSTANCE),
        UUID.randomUUID().toString());
  }

  @Test public void testGetSameThreadSeedGen() {
    SeedGenerator seedGen = getTestSeedGenerator();
    EntropyBlockingRandomWrapper random = new EntropyBlockingRandomWrapper(0L, seedGen);
    assertSame(random.getSameThreadSeedGen(), seedGen);
  }

  @Override public void testSetSeedLong() throws SeedException {
    final BaseRandom rng = createRngLargeEntropyLimit();
    final BaseRandom rng2 = createRngLargeEntropyLimit();
    checkSetSeedLong(rng, rng2);
  }

  @Override @Test public void testReseeding() {
    // TODO
    SeedGenerator seedGen = Mockito.spy(getTestSeedGenerator());
    EntropyBlockingRandomWrapper random = new EntropyBlockingRandomWrapper(0L, seedGen);
    assertNull(random.getRandomSeeder());
    random.nextLong();
    Mockito.verify(seedGen, Mockito.atLeastOnce()).generateSeed(any(byte[].class));
    Mockito.verify(seedGen, Mockito.atMost(2)).generateSeed(any(byte[].class));
  }

  @Override protected RandomWrapper createRng() throws SeedException {
    return new EntropyBlockingRandomWrapper(EntropyBlockingTestUtils.DEFAULT_MAX_ENTROPY, getTestSeedGenerator());
  }

  private EntropyBlockingRandomWrapper createRngLargeEntropyLimit() {
    return new EntropyBlockingRandomWrapper(EntropyBlockingTestUtils.VERY_LOW_MINIMUM_ENTROPY, getTestSeedGenerator());
  }

  private EntropyBlockingRandomWrapper createRngLargeEntropyLimit(byte[] seed) {
    return new EntropyBlockingRandomWrapper(seed, EntropyBlockingTestUtils.VERY_LOW_MINIMUM_ENTROPY, getTestSeedGenerator());
  }

  // FIXME: Too slow!
  @Override @Test(timeOut = 120_000L) public void testRandomSeederThreadIntegration() {
    final SeedGenerator seedGenerator = new SemiFakeSeedGenerator(new Random(),
        UUID.randomUUID().toString());
    final BaseRandom rng = createRng();
    RandomTestUtils.checkReseeding(seedGenerator, rng, true, 64);
  }

  @Override protected RandomWrapper createRng(byte[] seed) throws SeedException {
    return new EntropyBlockingRandomWrapper(seed, EntropyBlockingTestUtils.DEFAULT_MAX_ENTROPY, getTestSeedGenerator());
  }

  @Override public void testThreadSafety() {
    testThreadSafetyVsCrashesOnly(30,
        ImmutableList.of(NEXT_LONG, NEXT_INT, NEXT_DOUBLE, NEXT_GAUSSIAN, SET_WRAPPED));
  }

  @Test public void testManualReseeding() {
    EntropyBlockingRandomWrapper random = new EntropyBlockingRandomWrapper(
        getTestSeedGenerator().generateSeed(8), 0L, null);
    random.nextInt();
    random.setSeed(getTestSeedGenerator().generateSeed(8));
    random.nextLong();
    try {
      random.nextInt();
      fail("Expected an IllegalStateException");
    } catch (IllegalStateException expected) {}
  }

  @Override protected RandomTestUtils.EntropyCheckMode getEntropyCheckMode() {
    return RandomTestUtils.EntropyCheckMode.LOWER_BOUND;
  }

  // FIXME: Gets interrupted
  @Test(retryAnalyzer = FlakyRetryAnalyzer.class) public void testRandomSeederThreadUsedFirst() {
    SeedGenerator testSeedGenerator = getTestSeedGenerator();
    SeedGenerator seederSeedGenSpy = Mockito.spy(testSeedGenerator);
    ThreadFactory defaultThreadFactory
        = new DefaultThreadFactory("testRandomSeederThreadUsedFirst", Thread.MAX_PRIORITY);
    SimpleRandomSeederThread seeder = new SimpleRandomSeederThread(seederSeedGenSpy,
        defaultThreadFactory);
    SemiFakeSeedGenerator sameThreadSeedGen
        = Mockito.spy(new SemiFakeSeedGenerator(new SplittableRandomAdapter(), "sameThreadSeedGen"));
    EntropyBlockingRandomWrapper random = new EntropyBlockingRandomWrapper(
        testSeedGenerator.generateSeed(8), 0L, sameThreadSeedGen);
    random.setRandomSeeder(seeder);
    random.nextLong();
    try {
      assertEquals(random.getSameThreadSeedGen(), sameThreadSeedGen,
          "Same-thread seed generator changed after setting RandomSeederThread, when already non-null");
      random.nextLong();
      Mockito.verify(seederSeedGenSpy, Mockito.atLeastOnce()).generateSeed(any(byte[].class));
      Mockito.verify(seederSeedGenSpy, Mockito.atMost(2)).generateSeed(any(byte[].class));
      Mockito.verify(sameThreadSeedGen, Mockito.never()).generateSeed(any(byte[].class));
      Mockito.verify(sameThreadSeedGen, Mockito.never()).generateSeed(anyInt());
    } finally {
      random.setRandomSeeder(null);
      seeder.stopIfEmpty();
    }
  }

  @Test(timeOut = 10_000L) public void testFallbackFromRandomSeederThread() {
    SeedGenerator failingSeedGen = Mockito.spy(FailingSeedGenerator.DEFAULT_INSTANCE);
    SimpleRandomSeederThread seeder = new SimpleRandomSeederThread(failingSeedGen);
    SeedGenerator sameThreadSeedGen
        = Mockito.spy(new SemiFakeSeedGenerator(new SplittableRandomAdapter()));
    EntropyBlockingRandomWrapper random = new EntropyBlockingRandomWrapper(0L, sameThreadSeedGen);
    random.setRandomSeeder(seeder);
    try {
      random.nextLong();
      Mockito.verify(sameThreadSeedGen, Mockito.atLeastOnce()).generateSeed(any(byte[].class));
      Mockito.verify(sameThreadSeedGen, Mockito.atMost(2)).generateSeed(any(byte[].class));
    } finally {
      random.setRandomSeeder(null);
      seeder.stopIfEmpty();
    }
  }

  @Override public void testSerializable() throws SeedException {
    testSerializable(createRngLargeEntropyLimit());
  }

  @Override public void testNextBytes() {
    final byte[] testBytes = new byte[TEST_BYTE_ARRAY_LENGTH];
    final BaseRandom prng = new EntropyBlockingRandomWrapper(0L, getTestSeedGenerator());
    final long oldEntropy = prng.getEntropyBits();
    prng.nextBytes(testBytes);
    assertFalse(Arrays.equals(testBytes, new byte[TEST_BYTE_ARRAY_LENGTH]));
  }

  @Override public void testSetSeedAfterNextLong() throws SeedException {
    checkSetSeedAfter(this::createRngLargeEntropyLimit, this::createRngLargeEntropyLimit,
        BaseRandom::nextLong);
  }

  @Override public void testSetSeedAfterNextInt() throws SeedException {
    checkSetSeedAfter(this::createRngLargeEntropyLimit, this::createRngLargeEntropyLimit,
        BaseRandom::nextInt);
  }

  @Test public void testSetSameThreadSeedGen() {
    SeedGenerator seedGen = Mockito.spy(getTestSeedGenerator());
    EntropyBlockingRandomWrapper random
        = new EntropyBlockingRandomWrapper(seedGen.generateSeed(8), 0L, null);
    random.setSameThreadSeedGen(seedGen);
    assertSame(random.getSameThreadSeedGen(), seedGen);
    random.nextLong();
    Mockito.verify(seedGen, Mockito.atLeastOnce()).generateSeed(any(byte[].class));
  }
}