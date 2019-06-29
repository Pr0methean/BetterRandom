package io.github.pr0methean.betterrandom.seed;

import com.google.common.testing.GcFinalization;
import io.github.pr0methean.betterrandom.prng.Pcg64Random;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.locks.LockSupport;

import static org.testng.Assert.*;

public class RandomSeederThreadTest {

  private static final long TEST_SEED = 0x0123456789ABCDEFL;
  private static final int TEST_OUTPUT_SIZE = 20;

  @Test(timeOut = 25_000) public void testAddRemoveAndIsEmpty() throws Exception {
    final Random prng = new Random(TEST_SEED);
    final byte[] bytesWithOldSeed = new byte[TEST_OUTPUT_SIZE];
    prng.nextBytes(bytesWithOldSeed);
    prng.setSeed(TEST_SEED); // Rewind
    final SeedGenerator seedGenerator = new FakeSeedGenerator("testAddRemoveAndIsEmpty");
    final RandomSeederThread randomSeeder = new RandomSeederThread(seedGenerator);
    try {
      assertTrue(randomSeeder.isEmpty());
      randomSeeder.add(prng);
      assertFalse(randomSeeder.isEmpty());
      sleepUninterruptibly(100_000_000); // FIXME: Why does this sleep get interrupted?!
      assertFalse(randomSeeder.isEmpty());
    } finally {
      RandomTestUtils.removeAndAssertEmpty(randomSeeder, prng);
    }
    final byte[] bytesWithNewSeed = new byte[TEST_OUTPUT_SIZE];
    prng.nextBytes(bytesWithNewSeed);
    assertFalse(Arrays.equals(bytesWithOldSeed, bytesWithNewSeed));
  }

  @Test public void testResurrection() throws InterruptedException {
    final FakeSeedGenerator seedGenerator = new FakeSeedGenerator("testResurrection");
    seedGenerator.setThrowException(true);
    final RandomSeederThread randomSeeder = new RandomSeederThread(seedGenerator);
    try {
      Random random = new Pcg64Random();
      randomSeeder.add(random);
      try {
        random.nextLong();
        random.nextLong();
        Thread.sleep(100);
        assertFalse(randomSeeder.isRunning());
        assertEquals(seedGenerator.countCalls(), 1);
        seedGenerator.setThrowException(false);
        randomSeeder.remove(random);
        randomSeeder.add(random);
        random.nextBoolean();
        Thread.sleep(100);
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

  @Test public void testStopIfEmpty() throws InterruptedException {
    // FIXME: When the commented lines are uncommented, the ref never gets queued!
    final SeedGenerator seedGenerator = new FakeSeedGenerator("testStopIfEmpty");
    final RandomSeederThread randomSeeder = new RandomSeederThread(seedGenerator);
    // ReferenceQueue<Object> queue = new ReferenceQueue<>();
    addSomethingDeadTo(randomSeeder);
    GcFinalization.awaitFullGc();
    Thread.sleep(1000); // FIXME: Why is this needed?
    // assertNotNull(queue.remove(10_000));
    randomSeeder.stopIfEmpty();
    assertFalse(randomSeeder.isRunning());
  }

  /** Making this a subroutine ensures that {@code prng} can be GCed on exit. */
  private void addSomethingDeadTo(RandomSeederThread randomSeeder) {
    Random prng = new Random();
    // new PhantomReference<Object>(prng, queue);
    randomSeeder.add(prng);
    randomSeeder.stopIfEmpty();
    assertTrue(randomSeeder.isRunning());
    prng.nextBoolean(); // could replace with Reference.reachabilityFence if JDK8 support wasn't needed
  }

  private void sleepUninterruptibly(long nanos) {
    long curTime = System.nanoTime();
    long endTime = curTime + nanos;
    do {
      LockSupport.parkNanos(endTime - curTime);
      curTime = System.nanoTime();
    } while (curTime < endTime);
  }
}
