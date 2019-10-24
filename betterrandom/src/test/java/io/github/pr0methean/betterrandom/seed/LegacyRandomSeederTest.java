package io.github.pr0methean.betterrandom.seed;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;
import io.github.pr0methean.betterrandom.TestUtils;
import io.github.pr0methean.betterrandom.prng.RandomTestUtils;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java8.util.function.Consumer;
import org.testng.annotations.Test;

public class LegacyRandomSeederTest extends SimpleRandomSeederTest {
  @Override @Test public void testConstructors() {
    TestUtils.testConstructors(LegacyRandomSeeder.class, false, ImmutableMap
        .of(SeedGenerator.class, new FakeSeedGenerator("testConstructors"), ThreadFactory.class,
            new SimpleRandomSeeder.DefaultThreadFactory("testConstructors"), long.class,
            100_000_000L), new Consumer<LegacyRandomSeeder>() {
      @Override public void accept(LegacyRandomSeeder legacyRandomSeeder) {
        legacyRandomSeeder.stopIfEmpty();
      }
    });
  }

  @Override protected LegacyRandomSeeder createRandomSeeder(SeedGenerator seedGenerator) {
    return new LegacyRandomSeeder(seedGenerator,
        new SimpleRandomSeeder.DefaultThreadFactory("LegacyRandomSeederTest", Thread.MAX_PRIORITY));
  }

  @Test(timeOut = 25_000) public void testAddRemoveAndIsEmpty_Random() {
    final Random prng = new Random(TEST_SEED);
    final byte[] firstBytesWithOldSeed = new byte[TEST_OUTPUT_SIZE];
    final byte[] secondBytesWithOldSeed = new byte[TEST_OUTPUT_SIZE];
    prng.nextBytes(firstBytesWithOldSeed);
    prng.nextBytes(secondBytesWithOldSeed);
    prng.setSeed(TEST_SEED); // Rewind
    final SeedGenerator seedGenerator = new FakeSeedGenerator("testAddRemoveAndIsEmpty");
    final LegacyRandomSeeder randomSeeder = createRandomSeeder(seedGenerator);
    try {
      assertTrue(randomSeeder.isEmpty());
      randomSeeder.add(prng);
      assertFalse(randomSeeder.isEmpty());
      prng.nextBytes(new byte[TEST_OUTPUT_SIZE]); // Drain the entropy
      // FIXME: Why does this sleep get interrupted?!
      Uninterruptibles.sleepUninterruptibly(1000L, TimeUnit.MILLISECONDS);
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
}
