package io.github.pr0methean.betterrandom.prng.concurrent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

import com.google.common.collect.ImmutableList;
import io.github.pr0methean.betterrandom.DeadlockWatchdogThread;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.seed.FailingSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SemiFakeSeedGenerator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadFactory;
import org.mockito.Mockito;
import org.testng.annotations.Test;

@Test(testName = "EntropyBlockingRandomWrapper")
public class EntropyBlockingRandomWrapperTest extends RandomWrapperRandomTest {
  private static final long DEFAULT_MAX_ENTROPY = -1000L;

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
    return new SemiFakeSeedGenerator(
        new SplittableRandomAdapter(SecureRandomSeedGenerator.SECURE_RANDOM_SEED_GENERATOR),
        UUID.randomUUID().toString());
  }

  @Test public void testGetSameThreadSeedGen() {
    SeedGenerator seedGen = getTestSeedGenerator();
    EntropyBlockingRandomWrapper random = new EntropyBlockingRandomWrapper(0L, seedGen);
    assertSame(random.getSameThreadSeedGen(), seedGen);
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

  // FIXME: Too slow!
  @Override @Test(timeOut = 120_000L) public void testRandomSeederThreadIntegration() {
    super.testRandomSeederThreadIntegration();
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

  @Test(timeOut = 60_000L) public void testRandomSeederThreadUsedFirst() {
    DeadlockWatchdogThread.ensureStarted();
    try {
      SeedGenerator testSeedGenerator = getTestSeedGenerator();
      SeedGenerator seederSeedGenSpy = Mockito.spy(testSeedGenerator);
      final ThreadFactory defaultThreadFactory = new RandomSeederThread.DefaultThreadFactory("testRandomSeederThreadUsedFirst");
      RandomSeederThread seeder = new RandomSeederThread(seederSeedGenSpy, new ThreadFactory() {
        @Override public Thread newThread(Runnable runnable) {
          Thread thread = defaultThreadFactory.newThread(runnable);
          thread.setPriority(Thread.MAX_PRIORITY);
          return thread;
        }
      });
      SemiFakeSeedGenerator sameThreadSeedGen = Mockito
          .spy(new SemiFakeSeedGenerator(new SplittableRandomAdapter(), "sameThreadSeedGen"));
      EntropyBlockingRandomWrapper random =
          new EntropyBlockingRandomWrapper(testSeedGenerator.generateSeed(8), 0L, sameThreadSeedGen);
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
        seeder.shutDown();
      }
    } finally {
      DeadlockWatchdogThread.stopInstance();
    }
  }

  @Test(timeOut = 20_000L) public void testFallbackFromRandomSeederThread() {
    SeedGenerator failingSeedGen = Mockito.spy(new FailingSeedGenerator());
    RandomSeederThread seeder = new RandomSeederThread(failingSeedGen);
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
      seeder.shutDown();
    }
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