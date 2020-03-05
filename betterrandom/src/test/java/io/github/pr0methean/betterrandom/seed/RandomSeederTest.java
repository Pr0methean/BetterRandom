package io.github.pr0methean.betterrandom.seed;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.testing.GcFinalization;
import com.google.common.util.concurrent.Uninterruptibles;
import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import io.github.pr0methean.betterrandom.FlakyRetryAnalyzer;
import io.github.pr0methean.betterrandom.TestUtils;
import io.github.pr0methean.betterrandom.prng.Pcg64Random;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import io.github.pr0methean.betterrandom.prng.adapter.SingleThreadSplittableRandomAdapter;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.Test;

public class RandomSeederTest {
  protected static final long TEST_SEED = 0x0123456789ABCDEFL;
  protected static final int TEST_OUTPUT_SIZE = 20;

  @Test public void testConstructors() {
    TestUtils.testConstructors(RandomSeeder.class, false, ImmutableMap
        .of(SeedGenerator.class, new FakeSeedGenerator("testConstructors"),
            ThreadFactory.class, new RandomSeeder.DefaultThreadFactory("testConstructors"),
            long.class, 100_000_000L),
        RandomSeeder::stopIfEmpty);
  }

  @Test public void testDefaultThreadFactoryConstructors() {
    TestUtils.testConstructors(RandomSeeder.DefaultThreadFactory.class, false, ImmutableMap
            .of(String.class, "testDefaultThreadFactoryConstructors",
                int.class, Thread.MAX_PRIORITY),
        x -> x.newThread(() -> {}));
  }

  @Test(timeOut = 25_000) public void testAddRemoveAndIsEmpty() {
    final SingleThreadSplittableRandomAdapter prng
        = new SingleThreadSplittableRandomAdapter(TEST_SEED);
    final byte[] firstBytesWithOldSeed = new byte[TEST_OUTPUT_SIZE];
    final byte[] secondBytesWithOldSeed = new byte[TEST_OUTPUT_SIZE];
    prng.nextBytes(firstBytesWithOldSeed);
    prng.nextBytes(secondBytesWithOldSeed);
    prng.setSeed(TEST_SEED); // Rewind
    final SeedGenerator seedGenerator = new SemiFakeSeedGenerator(
        new SingleThreadSplittableRandomAdapter(), "testAddRemoveAndIsEmpty");
    final RandomSeeder randomSeeder = createRandomSeeder(seedGenerator);
    try {
      assertTrue(randomSeeder.isEmpty());
      randomSeeder.add(prng);
      assertFalse(randomSeeder.isEmpty());
      prng.nextBytes(new byte[TEST_OUTPUT_SIZE]); // Drain the entropy
      // FIXME: Why does this sleep get interrupted?!
      Uninterruptibles.sleepUninterruptibly(2000L, TimeUnit.MILLISECONDS);
      assertFalse(randomSeeder.isEmpty());
    } finally {
      RandomTestUtils.removeAndAssertEmpty(randomSeeder, prng);
    }
    final byte[] bytesWithNewSeed = new byte[TEST_OUTPUT_SIZE];
    prng.nextBytes(bytesWithNewSeed);
    assertFalse(Arrays.equals(firstBytesWithOldSeed, bytesWithNewSeed),
        "Repeated output after reseeding");
    assertFalse(Arrays.equals(secondBytesWithOldSeed, bytesWithNewSeed),
        "Repeated output after reseeding");
  }

  @Test(retryAnalyzer = FlakyRetryAnalyzer.class) public void testResurrection()
      throws InterruptedException {
    final FakeSeedGenerator seedGenerator = new FakeSeedGenerator("testResurrection");
    seedGenerator.setThrowException(true);
    final RandomSeeder randomSeeder = createRandomSeeder(seedGenerator);
    try {
      Pcg64Random random = new Pcg64Random();
      randomSeeder.add(random);
      try {
        random.nextLong();
        random.nextLong();
        Thread.sleep(500);
        assertFalse(randomSeeder.isRunning());
        assertEquals(seedGenerator.countCalls(), 1);
        seedGenerator.setThrowException(false);
        randomSeeder.remove(random);
        randomSeeder.add(random);
        random.nextBoolean();
        Thread.sleep(200);
        assertTrue(randomSeeder.isRunning());
        assertEquals(seedGenerator.countCalls(), 2);
        random.nextBoolean();
      } finally {
        randomSeeder.remove(random);
      }
    } finally {
      randomSeeder.stopIfEmpty();
    }
  }

  @Test(singleThreaded = true, retryAnalyzer = FlakyRetryAnalyzer.class)
  public void testStopIfEmpty() throws InterruptedException {
    // FIXME: When the commented lines are uncommented, the ref never gets queued!
    final SeedGenerator seedGenerator = new FakeSeedGenerator("testStopIfEmpty");
    final RandomSeeder randomSeeder = createRandomSeeder(seedGenerator);
    // ReferenceQueue<Object> queue = new ReferenceQueue<>();
    GcFinalization.awaitClear(addSomethingDeadTo(randomSeeder));
    Thread.sleep(1000); // FIXME: Why is this needed?
    // assertNotNull(queue.remove(10_000));
    randomSeeder.stopIfEmpty();
    assertFalse(randomSeeder.isRunning(), "randomSeeder did not stop");
  }

  protected RandomSeeder createRandomSeeder(SeedGenerator seedGenerator) {
    return new RandomSeeder(seedGenerator,
        new RandomSeeder.DefaultThreadFactory("RandomSeederTest", Thread.MAX_PRIORITY));
  }

  /**
   * Making this a subroutine ensures that {@code prng} can be GCed on exit.
   */
  private WeakReference<ByteArrayReseedableRandom>
      addSomethingDeadTo(RandomSeeder randomSeeder) {
    SingleThreadSplittableRandomAdapter prng = new SingleThreadSplittableRandomAdapter(TEST_SEED);
    randomSeeder.add(prng);
    randomSeeder.stopIfEmpty();
    assertTrue(randomSeeder.isRunning());
    prng.nextBoolean(); // could replace with Reference.reachabilityFence if JDK8 support wasn't
    // needed
    return new WeakReference<>(prng);
  }
}
