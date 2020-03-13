package io.github.pr0methean.betterrandom.seed;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import com.google.common.util.concurrent.Uninterruptibles;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import java.util.concurrent.CountDownLatch;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(singleThreaded = true) public class BufferedSeedGeneratorTest
    extends SeedGeneratorTest<BufferedSeedGenerator> {

  private static final FakeSeedGenerator FAKE_SEED_GENERATOR = new FakeSeedGenerator();
  private static final int BUFFER_SIZE = 256;
  private static final int THREAD_COUNT = 4;
  private static final int CONCURRENT_SEED_SIZE = 16;

  public BufferedSeedGeneratorTest() {
    super(null);
  }

  @BeforeMethod public void setUp() {
    seedGenerator = new BufferedSeedGenerator(FAKE_SEED_GENERATOR, BUFFER_SIZE);
    FAKE_SEED_GENERATOR.reset();
  }

  @Test public void testLargeRequestDoneAsOne() {
    generateAndCheckFakeSeed(2 * BUFFER_SIZE);
    assertEquals(FAKE_SEED_GENERATOR.countCalls(), 1);
    generateAndCheckFakeSeed(BUFFER_SIZE / 2);
    assertEquals(FAKE_SEED_GENERATOR.countCalls(), 2);
  }

  @Test public void testSmallRequests() {
    final int smallRequestSize = BUFFER_SIZE / 2;
    generateAndCheckFakeSeed(smallRequestSize);
    assertEquals(FAKE_SEED_GENERATOR.countCalls(), 1);
    generateAndCheckFakeSeed(smallRequestSize, smallRequestSize);
    assertEquals(FAKE_SEED_GENERATOR.countCalls(), 1);
    generateAndCheckFakeSeed(smallRequestSize);
    assertEquals(FAKE_SEED_GENERATOR.countCalls(), 2);
  }

  @Test public void testThreadSafety() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
    Thread[] threads = new Thread[THREAD_COUNT];
    byte[][] seeds = new byte[THREAD_COUNT][CONCURRENT_SEED_SIZE];
    for (int i = 0; i < THREAD_COUNT; i++) {
      final int threadNum = i;
      threads[i] = new Thread(() -> {
        latch.countDown();
        Uninterruptibles.awaitUninterruptibly(latch);
        seedGenerator.generateSeed(seeds[threadNum]);
      });
      threads[i].start();
    }
    for (int i = 0; i < THREAD_COUNT; i++) {
      threads[i].join();
    }
    StringBuilder seedList = new StringBuilder();
    for (int i = 0; i < THREAD_COUNT; i++) {
      seedList.append(BinaryUtils.convertBytesToHexString(seeds[i])).append('\n');
    }
    // If the same byte occurs twice, seeds overlap
    int[] counts = new int[256];
    for (int thread = 0; thread < THREAD_COUNT; thread++) {
      for (int i = 0; i < CONCURRENT_SEED_SIZE; i++) {
        int value = seeds[thread][i];
        if (++(counts[value - Byte.MIN_VALUE]) > 1) {
          fail("Same seed byte returned to two threads; seeds are:\n" + seedList);
        }
      }
    }
    assertEquals(FAKE_SEED_GENERATOR.countCalls(), 1, "Should have had to refill only once");
  }

  @Test public void testThreadSafetyWithRefillInMidSeed() throws InterruptedException {
    seedGenerator.generateSeed(BUFFER_SIZE - 13);
    FAKE_SEED_GENERATOR.reset();
    testThreadSafety();
  }
}
