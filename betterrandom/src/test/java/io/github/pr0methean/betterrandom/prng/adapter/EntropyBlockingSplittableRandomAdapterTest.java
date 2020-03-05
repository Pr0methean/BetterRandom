package io.github.pr0methean.betterrandom.prng.adapter;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.DEFAULT_INSTANCE;
import static org.mockito.ArgumentMatchers.any;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;

import com.google.common.testing.SerializableTester;
import com.google.common.util.concurrent.Uninterruptibles;
import io.github.pr0methean.betterrandom.FlakyRetryAnalyzer;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import io.github.pr0methean.betterrandom.seed.FakeSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeeder;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SemiFakeSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeeder.DefaultThreadFactory;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class EntropyBlockingSplittableRandomAdapterTest
    extends SplittableRandomAdapterTest {

  private RandomSeeder thread;

  // FIXME: Why does this need more time than other PRNGs?!
  @Override @Test(timeOut = 80_000) public void testNextGaussianStatistically() throws SeedException {
    super.testNextGaussianStatistically();
  }

  @Override @BeforeMethod public void setUp() {
    thread = new RandomSeeder(getTestSeedGenerator(),
        new DefaultThreadFactory("EntropyBlockingReseedingSplittableRandomAdapterTest",
            Thread.MAX_PRIORITY));
  }

  @Override @AfterMethod public void tearDown() {
    thread.stopIfEmpty();
    thread = null;
  }

  @Override protected EntropyBlockingSplittableRandomAdapter createRng()
      throws SeedException {
    return new EntropyBlockingSplittableRandomAdapter(getTestSeedGenerator(), thread,
        EntropyBlockingTestUtils.DEFAULT_MAX_ENTROPY);
  }

  @Override protected BaseRandom createRng(byte[] seed) throws SeedException {
    EntropyBlockingSplittableRandomAdapter out = createRng();
    out.setSeed(seed);
    return out;
  }

  @Override public Map<Class<?>, Object> constructorParams() {
    Map<Class<?>, Object> out = super.constructorParams();
    out.put(long.class, EntropyBlockingTestUtils.DEFAULT_MAX_ENTROPY);
    out.put(RandomSeeder.class, thread);
    return out;
  }

  // FIXME: Why does this need so much more time than other PRNGs?!
  @Test(timeOut = 240_000, retryAnalyzer = FlakyRetryAnalyzer.class)
  @Override public void testDistribution() throws SeedException {
    super.testDistribution();
  }

  @Override public void testInitialEntropy() {
    // This test needs a separate instance from all other tests, but createRng() doesn't provide one
    SplittableRandomAdapter random = createRng();
    assertEquals(random.getEntropyBits(), Long.SIZE, "Wrong initial entropy");
  }

  // FIXME: Why does this need so much more time than other PRNGs?!
  @Test(timeOut = 120_000, retryAnalyzer = FlakyRetryAnalyzer.class)
  @Override public void testIntegerSummaryStats() throws SeedException {
    super.testIntegerSummaryStats();
  }

  @Override @Test public void testSerializable() throws SeedException {
    SeedGenerator generator = new FakeSeedGenerator("testSerializable");
    RandomSeeder thread = new RandomSeeder(generator);
    try {
      final BaseSplittableRandomAdapter adapter =
          new EntropyBlockingSplittableRandomAdapter(
              generator, thread, EntropyBlockingTestUtils.DEFAULT_MAX_ENTROPY);
      final BaseSplittableRandomAdapter clone = SerializableTester.reserialize(adapter);
      assertEquals(adapter, clone);
    } finally {
      thread.shutDown();
    }
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return EntropyBlockingSplittableRandomAdapter.class;
  }

  @Override @Test(retryAnalyzer = FlakyRetryAnalyzer.class) public void testReseeding() {
    SeedGenerator generator =
        new SemiFakeSeedGenerator(ThreadLocalRandom.current(), "testReseeding");
    RandomSeeder seeder = new RandomSeeder(generator);
    try {
      EntropyBlockingSplittableRandomAdapter random =
          new EntropyBlockingSplittableRandomAdapter(generator, seeder,
              EntropyBlockingTestUtils.DEFAULT_MAX_ENTROPY);
      RandomTestUtils.checkReseeding(generator, random, false);
    } finally {
      seeder.shutDown();
    }
  }

  /**
   * Test for crashes only, since setSeed is a no-op.
   */
  @Override @Test public void testSetSeedAfterNextLong() throws SeedException {
    final BaseRandom prng = createRng();
    prng.nextLong();
    prng.setSeed(getTestSeedGenerator().generateSeed(8));
    prng.setSeed(BinaryUtils.convertBytesToLong(getTestSeedGenerator().generateSeed(8)));
    prng.nextLong();
  }

  /**
   * Test for crashes only, since setSeed is a no-op.
   */
  @Override @Test public void testSetSeedAfterNextInt() throws SeedException {
    final BaseRandom prng = createRng();
    prng.nextInt();
    prng.setSeed(getTestSeedGenerator().generateSeed(8));
    prng.setSeed(BinaryUtils.convertBytesToLong(getTestSeedGenerator().generateSeed(8)));
    prng.nextInt();
  }

  /**
   * Assertion-free since reseeding may cause divergent output.
   */
  @Override @Test(timeOut = 10000) public void testSetSeedLong() {
    createRng().setSeed(0x0123456789ABCDEFL);
  }

  /**
   * setRandomSeeder doesn't work on this class and shouldn't pretend to.
   */
  @Override @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testRandomSeederThreadIntegration() {
    RandomSeeder thread = new RandomSeeder(DEFAULT_INSTANCE);
    try {
      createRng().setRandomSeeder(thread);
    } finally {
      thread.stopIfEmpty();
    }
  }

  @Test public void testSetSeedGeneratorNoOp() {
    createRng().setRandomSeeder(thread);
  }

  @Override @Test public void testDump() throws SeedException {
    RandomSeeder thread = new RandomSeeder(DEFAULT_INSTANCE);
    try {
      EntropyBlockingSplittableRandomAdapter firstInstance =
          new EntropyBlockingSplittableRandomAdapter(getTestSeedGenerator(), thread,
                        EntropyBlockingTestUtils.DEFAULT_MAX_ENTROPY);
      RandomSeeder otherThread =
          new RandomSeeder(new FakeSeedGenerator("Different reseeder"));
      try {
        EntropyBlockingSplittableRandomAdapter secondInstance =
            new EntropyBlockingSplittableRandomAdapter(getTestSeedGenerator(), otherThread,
                EntropyBlockingTestUtils.DEFAULT_MAX_ENTROPY);
        assertNotEquals(secondInstance.dump(), firstInstance.dump());
      } finally {
        otherThread.shutDown();
      }
    } finally {
      thread.shutDown();
    }
  }

  @Test public void testRandomSeederThreadUsedFirst() {
    SeedGenerator testSeedGenerator = getTestSeedGenerator();
    SeedGenerator seederSeedGenSpy = Mockito.spy(testSeedGenerator);
    ThreadFactory defaultThreadFactory
        = new DefaultThreadFactory("testRandomSeederThreadUsedFirst", Thread.MAX_PRIORITY);
    RandomSeeder seeder = new RandomSeeder(seederSeedGenSpy,
        defaultThreadFactory);
    SemiFakeSeedGenerator sameThreadSeedGen
        = new SemiFakeSeedGenerator(ThreadLocalRandom.current(), "sameThreadSeedGen");
    EntropyBlockingSplittableRandomAdapter random = new EntropyBlockingSplittableRandomAdapter(
        sameThreadSeedGen, seeder, 0L);
    random.nextLong();
    sameThreadSeedGen.setThrowException(true);
    try {
      random.nextLong();
      Mockito.verify(seederSeedGenSpy, Mockito.atLeastOnce()).generateSeed(any(byte[].class));
      Mockito.verify(seederSeedGenSpy, Mockito.atMost(2)).generateSeed(any(byte[].class));
    } finally {
      seeder.shutDown();
    }
  }

  @SuppressWarnings("MismatchedReadAndWriteOfArray")
  @Test(timeOut = 10_000L) public void testGetSeed() {
    EntropyBlockingSplittableRandomAdapter random = createRng();
    byte[] seed = ((BaseRandom) random).getSeed();
    byte[] seed2 = new byte[Long.BYTES];
    byte[] zero = new byte[Long.BYTES];
    Thread newThread = new Thread(
        () -> System.arraycopy(((BaseRandom) random).getSeed(), 0, seed2, 0, Long.BYTES));
    newThread.start();
    Uninterruptibles.joinUninterruptibly(newThread); // FIXME: Spurious interrupts
    assertFalse(Arrays.equals(seed, seed2), "Same seed returned on different threads");
    assertFalse(Arrays.equals(zero, seed2), "Failed to copy to seed2");
  }
}
