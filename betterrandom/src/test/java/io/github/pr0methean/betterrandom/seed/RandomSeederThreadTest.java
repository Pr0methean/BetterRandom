package io.github.pr0methean.betterrandom.seed;

import org.testng.annotations.Test;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Random;

import static org.testng.Assert.*;

public class RandomSeederThreadTest {

  @Test public void testStopIfEmpty() throws InterruptedException {
    final SeedGenerator seedGenerator = new FakeSeedGenerator("testStopIfEmpty");
    final RandomSeederThread randomSeeder = new RandomSeederThread(seedGenerator);
    ReferenceQueue<Object> queue = new ReferenceQueue<>();
    addPrng(randomSeeder, queue);
    System.gc();
    assertNotNull(queue.remove(10_000));
    randomSeeder.stopIfEmpty();
    assertFalse(randomSeeder.isRunning());
  }

  /**
   * Must be a separate method so that {@code prng} will die on return.
   */
  private void addPrng(RandomSeederThread randomSeeder, ReferenceQueue<Object> queue) {
    Random prng = new Random();
    new PhantomReference<Object>(prng, queue);
    randomSeeder.add(prng);
    randomSeeder.stopIfEmpty();
    assertTrue(randomSeeder.isRunning());
    prng.nextBoolean(); // could replace with Reference.reachabilityFence if JDK8 support wasn't needed
  }
}
