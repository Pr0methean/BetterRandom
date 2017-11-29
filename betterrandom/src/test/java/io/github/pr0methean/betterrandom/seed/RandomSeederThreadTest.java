package io.github.pr0methean.betterrandom.seed;

import static io.github.pr0methean.betterrandom.seed.RandomSeederThread.stopAllEmpty;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Random;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

public class RandomSeederThreadTest {

  private static final long TEST_SEED = 0x0123456789ABCDEFL;
  private static final int TEST_OUTPUT_SIZE = 20;

  @Test public void testAddRemoveAndIsEmpty() throws Exception {
    final Random prng = new Random(TEST_SEED);
    final byte[] bytesWithOldSeed = new byte[TEST_OUTPUT_SIZE];
    prng.nextBytes(bytesWithOldSeed);
    prng.setSeed(TEST_SEED); // Rewind
    final SeedGenerator seedGenerator = new FakeSeedGenerator();
    assertTrue(RandomSeederThread.isEmpty(seedGenerator));
    RandomSeederThread.add(seedGenerator, prng);
    assertFalse(RandomSeederThread.isEmpty(seedGenerator));
    Thread.sleep(1000);
    assertFalse(RandomSeederThread.isEmpty(seedGenerator));
    RandomSeederThread.remove(seedGenerator, prng);
    assertTrue(RandomSeederThread.isEmpty(seedGenerator));
    final byte[] bytesWithNewSeed = new byte[TEST_OUTPUT_SIZE];
    prng.nextBytes(bytesWithNewSeed);
    assertFalse(Arrays.equals(bytesWithOldSeed, bytesWithNewSeed));
  }

  @Test public void testStopIfEmpty() throws Exception {
    final SeedGenerator seedGenerator = new FakeSeedGenerator();
    final Random prng = new Random();
    RandomSeederThread.add(seedGenerator, prng);
    RandomSeederThread.stopIfEmpty(seedGenerator);
    assertTrue(RandomSeederThread.hasInstance(seedGenerator));
    RandomSeederThread.remove(seedGenerator, prng);
    RandomSeederThread.stopIfEmpty(seedGenerator);
    Thread.sleep(1000);
    assertFalse(RandomSeederThread.hasInstance(seedGenerator));
  }

  @Test public void testStopAllEmpty() throws Exception {
    final SeedGenerator neverAddedTo = new FakeSeedGenerator();
    final SeedGenerator addedToAndRemoved = new FakeSeedGenerator();
    final SeedGenerator addedToAndLeft = new FakeSeedGenerator();
    final Random addedAndRemoved = new Random();
    final Random addedAndLeft = new Random();
    RandomSeederThread.add(addedToAndRemoved, addedAndRemoved);
    RandomSeederThread.remove(addedToAndRemoved, addedAndRemoved);
    RandomSeederThread.add(addedToAndLeft, addedAndLeft);
    assertFalse(RandomSeederThread.hasInstance(neverAddedTo));
    assertTrue(RandomSeederThread.hasInstance(addedToAndRemoved));
    assertTrue(RandomSeederThread.hasInstance(addedToAndLeft));
    stopAllEmpty();
    Thread.sleep(500);
    assertFalse(RandomSeederThread.hasInstance(neverAddedTo));
    assertFalse(RandomSeederThread.hasInstance(addedToAndRemoved));
    assertTrue(RandomSeederThread.hasInstance(addedToAndLeft));
    addedAndLeft.nextInt(); // prevent GC before this point
  }

  @AfterClass public void classTearDown() {
    System.gc();
    stopAllEmpty();
  }
}