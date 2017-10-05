package io.github.pr0methean.betterrandom.seed;

import static io.github.pr0methean.betterrandom.seed.RandomSeederThread.getInstance;
import static io.github.pr0methean.betterrandom.seed.RandomSeederThread.stopAllEmpty;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

import java.lang.Thread.State;
import java.util.Arrays;
import java.util.Random;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

public class RandomSeederThreadTest {

  private static final long TEST_SEED = 0x0123456789ABCDEFL;
  private static final int TEST_OUTPUT_SIZE = 20;

  @Test
  public void testGetInstance() throws Exception {
    assertNotSame(getInstance(RandomDotOrgSeedGenerator.DELAYED_RETRY),
        getInstance(RandomDotOrgSeedGenerator.RANDOM_DOT_ORG_SEED_GENERATOR));
  }

  @Test
  public void testAddRemoveAndIsEmpty() throws Exception {
    final Random prng = new Random(TEST_SEED);
    final byte[] bytesWithOldSeed = new byte[TEST_OUTPUT_SIZE];
    prng.nextBytes(bytesWithOldSeed);
    prng.setSeed(TEST_SEED); // Rewind
    final RandomSeederThread thread = getInstance(new FakeSeedGenerator());
    assertTrue(thread.isEmpty());
    thread.add(prng);
    assertFalse(thread.isEmpty());
    Thread.sleep(1000);
    assertFalse(thread.isEmpty());
    thread.remove(prng);
    assertTrue(thread.isEmpty());
    final byte[] bytesWithNewSeed = new byte[TEST_OUTPUT_SIZE];
    prng.nextBytes(bytesWithNewSeed);
    assertFalse(Arrays.equals(bytesWithOldSeed, bytesWithNewSeed));
  }

  @Test
  public void testStopIfEmpty() throws Exception {
    final RandomSeederThread thread = getInstance(new FakeSeedGenerator());
    final Random prng = new Random();
    thread.add(prng);
    thread.stopIfEmpty();
    assertTrue(thread.isAlive());
    thread.remove(prng);
    thread.stopIfEmpty();
    Thread.sleep(100);
    assertEquals(thread.getState(), State.TERMINATED);
  }

  @Test
  public void testStopAllEmpty() throws Exception {
    final RandomSeederThread neverAddedTo = getInstance(new FakeSeedGenerator());
    final RandomSeederThread addedToAndRemoved = getInstance(new FakeSeedGenerator());
    final RandomSeederThread addedToAndLeft = getInstance(new FakeSeedGenerator());
    final Random addedAndRemoved = new Random();
    final Random addedAndLeft = new Random();
    addedToAndRemoved.add(addedAndRemoved);
    addedToAndRemoved.remove(addedAndRemoved);
    addedToAndLeft.add(addedAndLeft);
    stopAllEmpty();
    Thread.sleep(100);
    assertEquals(neverAddedTo.getState(), State.TERMINATED);
    assertEquals(addedToAndRemoved.getState(), State.TERMINATED);
    assertTrue(addedToAndLeft.isAlive());
    System.out.println(addedAndLeft.nextInt()); // prevent GC before this point
  }

  @AfterClass
  public void classTearDown() {
    System.gc();
    stopAllEmpty();
  }
}