package io.github.pr0methean.betterrandom.prng.concurrent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.seed.FailingSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SemiFakeSeedGenerator;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.mockito.Mockito;
import org.testng.annotations.Test;

@Test(testName = "EntropyBlockingRandomWrapper")
public class EntropyBlockingRandomWrapperTest extends RandomWrapperRandomTest {
  private static final long DEFAULT_MAX_ENTROPY = -1000L;

  @Override public void testAllPublicConstructors()
      throws SeedException {
    mockDefaultSeedGenerator();
    try {
      for (final Constructor<?> constructor : getClassUnderTest().getDeclaredConstructors()) {
        final int modifiers = constructor.getModifiers();
        if (Modifier.isPublic(modifiers)) {
          constructor.setAccessible(true);
          final int nParams = constructor.getParameterCount();
          final Parameter[] parameters = constructor.getParameters();
          final Object[] constructorParams = new Object[nParams];
          try {
            for (int i = 0; i < nParams; i++) {
              if ("minimumEntropy".equals(parameters[i].getName())) {
                constructorParams[i] = DEFAULT_MAX_ENTROPY;
              } else {
                constructorParams[i] =
                    ((Map<Class<?>, Object>) ImmutableMap.copyOf(constructorParams()))
                        .get(parameters[i].getType());
              }
            }
            ((BaseRandom) constructor.newInstance(constructorParams)).nextInt();
          } catch (final IllegalAccessException | InstantiationException | InvocationTargetException | IllegalArgumentException e) {
            throw new AssertionError(String
                .format("Failed to call%n%s%nwith parameters%n%s", constructor.toGenericString(),
                    Arrays.toString(constructorParams)), e);
          }
        }
      }
    } finally {
      unmockDefaultSeedGenerator();
    }
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
    random.nextInt();
    try {
      random.nextInt();
      fail("Expected an IllegalStateException");
    } catch (IllegalStateException expected) {}
  }

  @Test(timeOut = 10_000L) public void testRandomSeederThreadUsedFirst() {
    SeedGenerator seederSeedGen = Mockito.spy(getTestSeedGenerator());
    RandomSeederThread seeder = new RandomSeederThread(seederSeedGen);
    SeedGenerator sameThreadSeedGen
        = Mockito.spy(new SemiFakeSeedGenerator(new SplittableRandomAdapter()));
    EntropyBlockingRandomWrapper random = new EntropyBlockingRandomWrapper(0L, sameThreadSeedGen);
    random.setRandomSeeder(seeder);
    try {
      assertEquals(random.getSameThreadSeedGen(), sameThreadSeedGen,
          "Same-thread seed generator changed after setting RandomSeederThread, when already non-null");
      random.nextLong();
      Mockito.verify(seederSeedGen, Mockito.atLeastOnce()).generateSeed(any(byte[].class));
      Mockito.verify(seederSeedGen, Mockito.atMost(2)).generateSeed(any(byte[].class));
      Mockito.verify(sameThreadSeedGen, Mockito.never()).generateSeed(any(byte[].class));
      Mockito.verify(sameThreadSeedGen, Mockito.never()).generateSeed(anyInt());
    } finally {
      random.setRandomSeeder(null);
      seeder.stopIfEmpty();
    }
  }

  @Test(timeOut = 10_000L) public void testFallbackFromRandomSeederThread() {
    SeedGenerator failingSeedGen = Mockito.spy(FailingSeedGenerator.DEFAULT_INSTANCE);
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
      seeder.stopIfEmpty();
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