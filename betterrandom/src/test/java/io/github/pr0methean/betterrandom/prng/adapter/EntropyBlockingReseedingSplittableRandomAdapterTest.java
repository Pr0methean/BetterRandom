package io.github.pr0methean.betterrandom.prng.adapter;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.DEFAULT_INSTANCE;
import static io.github.pr0methean.betterrandom.util.Java8Constants.LONG_BYTES;
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
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SemiFakeSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeeder;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeeder.DefaultThreadFactory;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class EntropyBlockingReseedingSplittableRandomAdapterTest
    extends ReseedingSplittableRandomAdapterTest {

  private SimpleRandomSeeder thread;

  // FIXME: Why does this need more time than other PRNGs?!
  @Override @Test(timeOut = 80_000) public void testNextGaussianStatistically() throws SeedException {
    super.testNextGaussianStatistically();
  }

  @Override @BeforeMethod public void setUp() {
    thread = new SimpleRandomSeeder(getTestSeedGenerator(),
        new DefaultThreadFactory("EntropyBlockingReseedingSplittableRandomAdapterTest",
            Thread.MAX_PRIORITY));
  }

  @Override @AfterMethod public void tearDown() {
    thread.stopIfEmpty();
    thread = null;
  }

  @Override protected EntropyBlockingReseedingSplittableRandomAdapter createRng()
      throws SeedException {
    return new EntropyBlockingReseedingSplittableRandomAdapter(getTestSeedGenerator(), thread,
        EntropyBlockingTestUtils.DEFAULT_MAX_ENTROPY);
  }

  @Override protected BaseRandom createRng(byte[] seed) throws SeedException {
    EntropyBlockingReseedingSplittableRandomAdapter out = createRng();
    out.setSeed(seed);
    return out;
  }

  @Override public Map<Class<?>, Object> constructorParams() {
    Map<Class<?>, Object> out = super.constructorParams();
    out.put(long.class, EntropyBlockingTestUtils.DEFAULT_MAX_ENTROPY);
    out.put(SimpleRandomSeeder.class, thread);
    return out;
  }

  // FIXME: Why does this need so much more time than other PRNGs?!
  @Test(timeOut = 240_000, retryAnalyzer = FlakyRetryAnalyzer.class)
  @Override public void testDistribution() throws SeedException {
    super.testDistribution();
  }

  @Override public void testInitialEntropy() {
    // This test needs a separate instance from all other tests, but createRng() doesn't provide one
    ReseedingSplittableRandomAdapter random = createRng();
    assertEquals(random.getEntropyBits(), Long.SIZE, "Wrong initial entropy");
  }

  // FIXME: Why does this need so much more time than other PRNGs?!
  @Test(timeOut = 120_000, retryAnalyzer = FlakyRetryAnalyzer.class)
  @Override public void testIntegerSummaryStats() throws SeedException {
    super.testIntegerSummaryStats();
  }

  @Override @Test public void testSerializable() throws SeedException {
    SeedGenerator generator = new FakeSeedGenerator("testSerializable");
    SimpleRandomSeeder thread = new SimpleRandomSeeder(generator);
    try {
      final BaseSplittableRandomAdapter adapter =
          new EntropyBlockingReseedingSplittableRandomAdapter(
              generator, thread, EntropyBlockingTestUtils.DEFAULT_MAX_ENTROPY);
      final BaseSplittableRandomAdapter clone = SerializableTester.reserialize(adapter);
      assertEquals(adapter, clone);
    } finally {
      thread.shutDown();
    }
  }

  @Override protected Class<? extends BaseRandom> getClassUnderTest() {
    return EntropyBlockingReseedingSplittableRandomAdapter.class;
  }

  @Override @Test(retryAnalyzer = FlakyRetryAnalyzer.class) public void testReseeding() {
    SeedGenerator generator =
        new SemiFakeSeedGenerator(ThreadLocalRandom.current(), "testReseeding");
    SimpleRandomSeeder seeder = new SimpleRandomSeeder(generator);
    try {
      EntropyBlockingReseedingSplittableRandomAdapter random =
          new EntropyBlockingReseedingSplittableRandomAdapter(generator, seeder,
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
    SimpleRandomSeeder thread = new SimpleRandomSeeder(DEFAULT_INSTANCE);
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
    SimpleRandomSeeder thread = new SimpleRandomSeeder(DEFAULT_INSTANCE);
    try {
      EntropyBlockingReseedingSplittableRandomAdapter firstInstance =
          new EntropyBlockingReseedingSplittableRandomAdapter(getTestSeedGenerator(), thread,
                        EntropyBlockingTestUtils.DEFAULT_MAX_ENTROPY);
      SimpleRandomSeeder otherThread =
          new SimpleRandomSeeder(new FakeSeedGenerator("Different reseeder"));
      try {
        EntropyBlockingReseedingSplittableRandomAdapter secondInstance =
            new EntropyBlockingReseedingSplittableRandomAdapter(getTestSeedGenerator(), otherThread,
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
    SimpleRandomSeeder seeder = new SimpleRandomSeeder(seederSeedGenSpy,
        defaultThreadFactory);
    SemiFakeSeedGenerator sameThreadSeedGen
        = new SemiFakeSeedGenerator(ThreadLocalRandom.current(), "sameThreadSeedGen");
    EntropyBlockingReseedingSplittableRandomAdapter random = new EntropyBlockingReseedingSplittableRandomAdapter(
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
    final EntropyBlockingReseedingSplittableRandomAdapter random = createRng();
    final byte[] seed = ((BaseRandom) random).getSeed();
    final byte[] seed2 = new byte[LONG_BYTES];
    final byte[] zero = new byte[LONG_BYTES];
    Thread newThread = new Thread() {
      @Override public void run() {
        System.arraycopy(((BaseRandom) random).getSeed(), 0, seed2, 0, LONG_BYTES);
      }
    };
    newThread.start();
    Uninterruptibles.joinUninterruptibly(newThread); // FIXME: Spurious interrupts
    assertFalse(Arrays.equals(seed, seed2), "Same seed returned on different threads");
    assertFalse(Arrays.equals(zero, seed2), "Failed to copy to seed2");
  }
}
