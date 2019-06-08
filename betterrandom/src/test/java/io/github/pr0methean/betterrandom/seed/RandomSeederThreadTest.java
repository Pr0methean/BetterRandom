package io.github.pr0methean.betterrandom.seed;

import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.locks.LockSupport;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RandomSeederThreadTest {

  private static final long TEST_SEED = 0x0123456789ABCDEFL;
  private static final int TEST_OUTPUT_SIZE = 20;

  private static final boolean ON_LINUX
      = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH)
          .contains("nux");

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
    if (ON_LINUX) {
      // FIXME: Fails without the Thread.sleep call
      assertFalse(Arrays.equals(bytesWithOldSeed, bytesWithNewSeed));
    }
  }

  @Test public void testStopIfEmpty() {
    final SeedGenerator seedGenerator = new FakeSeedGenerator("testStopIfEmpty");
    final RandomSeederThread randomSeeder = new RandomSeederThread(seedGenerator);
    final Random prng = new Random();
    randomSeeder.add(prng);
    randomSeeder.stopIfEmpty();
    // TODO: Assert stopped
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
