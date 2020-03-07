package io.github.pr0methean.betterrandom.prng.adapter;

import static io.github.pr0methean.betterrandom.prng.adapter.EntropyBlockingTestUtils.DEFAULT_MAX_ENTROPY;
import static io.github.pr0methean.betterrandom.prng.adapter.EntropyBlockingTestUtils.VERY_LOW_MINIMUM_ENTROPY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

import com.google.common.collect.ImmutableList;
import io.github.pr0methean.betterrandom.prng.AesCounterRandom;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import io.github.pr0methean.betterrandom.seed.FailingSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeeder;
import io.github.pr0methean.betterrandom.seed.RandomSeeder.DefaultThreadFactory;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SemiFakeSeedGenerator;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import org.mockito.Mockito;
import org.testng.annotations.Test;

@Test(testName = "EntropyBlockingRandomWrapper")
public class EntropyBlockingRandomWrapperTest extends RandomWrapperRandomTest {

  @Override public Class<? extends BaseRandom> getClassUnderTest() {
    return EntropyBlockingRandomWrapper.class;
  }

  @Override public Map<Class<?>, Object> constructorParams() {
    Map<Class<?>, Object> out = super.constructorParams();
    out.put(long.class, DEFAULT_MAX_ENTROPY);
    return out;
  }

  @Override protected SeedGenerator getTestSeedGenerator() {
    // Need a separate and non-value-equal instance for each test, for isolation
    return new SemiFakeSeedGenerator(ThreadLocalRandom.current(),
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
    return new EntropyBlockingRandomWrapper(DEFAULT_MAX_ENTROPY, getTestSeedGenerator());
  }

  private EntropyBlockingRandomWrapper createRngLargeEntropyLimit() {
    return new EntropyBlockingRandomWrapper(VERY_LOW_MINIMUM_ENTROPY, getTestSeedGenerator());
  }

  private EntropyBlockingRandomWrapper createRngLargeEntropyLimit(byte[] seed) {
    return new EntropyBlockingRandomWrapper(seed, VERY_LOW_MINIMUM_ENTROPY, getTestSeedGenerator());
  }

  // FIXME: Too slow!
  @Override @Test(timeOut = 120_000L) public void testRandomSeederIntegration() {
    final SeedGenerator seedGenerator = new SemiFakeSeedGenerator(new Random(),
        UUID.randomUUID().toString());
    final BaseRandom rng = createRng();
    RandomTestUtils.checkReseeding(seedGenerator, rng, true, 64);
  }

  @Override protected RandomWrapper createRng(byte[] seed) throws SeedException {
    return new EntropyBlockingRandomWrapper(seed, DEFAULT_MAX_ENTROPY, getTestSeedGenerator());
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

  @Test public void testReseedTriggeredAtZero() {
    SeedGenerator seedGenerator = getTestSeedGenerator();
    RandomSeeder seeder = Mockito.mock(RandomSeeder.class);
    AesCounterRandom wrapped = new AesCounterRandom(seedGenerator);
    int bytesToDrainToZero = (int) ((wrapped.getEntropyBits() + 7) / 8);
    EntropyBlockingRandomWrapper random =
        new EntropyBlockingRandomWrapper(wrapped, VERY_LOW_MINIMUM_ENTROPY, null);
    random.setRandomSeeder(seeder);
    Mockito.verify(seeder).add(random);
    Mockito.clearInvocations(seeder);
    random.nextBytes(new byte[bytesToDrainToZero]);
    Mockito.verify(seeder, Mockito.atLeastOnce()).wakeUp();
  }

  @Test public void testRandomSeederThreadUsedFirst() {
    SeedGenerator testSeedGenerator = getTestSeedGenerator();
    SeedGenerator seederSeedGenSpy = Mockito.spy(testSeedGenerator);
    ThreadFactory defaultThreadFactory
        = new DefaultThreadFactory("testRandomSeederThreadUsedFirst", Thread.MAX_PRIORITY);
    RandomSeeder seeder = new RandomSeeder(seederSeedGenSpy,
        defaultThreadFactory);
    SemiFakeSeedGenerator sameThreadSeedGen
        = Mockito.spy(new SemiFakeSeedGenerator(ThreadLocalRandom.current(), "sameThreadSeedGen"));
    EntropyBlockingRandomWrapper random = new EntropyBlockingRandomWrapper(
        testSeedGenerator.generateSeed(8), 0L, sameThreadSeedGen);
    random.setRandomSeeder(seeder);
    random.nextLong();
    try {
      assertEquals(random.getSameThreadSeedGen(), sameThreadSeedGen,
          "Same-thread seed generator changed after setting LegacyRandomSeeder, when already non-null");
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

  @Test(timeOut = 20_000L) public void testFallbackFromRandomSeederThread() {
    SeedGenerator failingSeedGen = Mockito.spy(FailingSeedGenerator.DEFAULT_INSTANCE);
    RandomSeeder seeder = new RandomSeeder(failingSeedGen);
    SeedGenerator sameThreadSeedGen
        = Mockito.spy(new SemiFakeSeedGenerator(ThreadLocalRandom.current()));
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

  @Test(timeOut = 10_000) public void testSetWrappedUnblocks() throws InterruptedException {
    RandomSeeder seeder = Mockito.mock(RandomSeeder.class);
    EntropyBlockingRandomWrapper random
        = new EntropyBlockingRandomWrapper(getTestSeedGenerator().generateSeed(8), 0L, null);
    random.setRandomSeeder(seeder);
    Thread consumer = new Thread(() -> random.nextBytes(new byte[9]));
    AtomicReference<Throwable> exception = new AtomicReference<>(null);
    consumer.setUncaughtExceptionHandler((thread, throwable) -> exception.set(throwable));
    consumer.start();
    while (random.getEntropyBits() > 0) {
      Thread.sleep(100);
    }
    random.setWrapped(new Random());
    consumer.join(1000);
    if (exception.get() != null) {
      fail("Consumer got exception", exception.get());
    }
    if (consumer.isAlive()) {
      Throwable stackTrace = new Throwable("Consumer stack trace");
      stackTrace.setStackTrace(consumer.getStackTrace());
      fail("Consumer is still running", stackTrace);
    }
    assertEquals(consumer.getState(), Thread.State.TERMINATED, "setWrapped didn't unblock");
  }
}
