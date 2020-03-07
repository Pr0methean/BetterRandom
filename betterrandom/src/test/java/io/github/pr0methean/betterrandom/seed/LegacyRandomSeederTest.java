package io.github.pr0methean.betterrandom.seed;

import com.google.common.collect.ImmutableMap;
import io.github.pr0methean.betterrandom.TestUtils;
import java.util.Random;
import java.util.concurrent.ThreadFactory;
import org.testng.annotations.Test;

public class LegacyRandomSeederTest extends RandomSeederTest {
  @Override @Test public void testConstructors() {
    TestUtils.testConstructors(LegacyRandomSeeder.class, false, ImmutableMap
        .of(SeedGenerator.class, new FakeSeedGenerator("testConstructors"), ThreadFactory.class,
            new RandomSeeder.DefaultThreadFactory("testConstructors"), long.class,
            100_000_000L), RandomSeeder::stopIfEmpty);
  }

  @Override protected LegacyRandomSeeder createRandomSeeder(SeedGenerator seedGenerator) {
    return new LegacyRandomSeeder(seedGenerator,
        new RandomSeeder.DefaultThreadFactory("LegacyRandomSeederTest", Thread.MAX_PRIORITY));
  }

  @Test(timeOut = 25_000) public void testAddRemoveAndIsEmpty_Random() {
    final Random prng = new Random(TEST_SEED);
    final SeedGenerator seedGenerator = new FakeSeedGenerator("testAddRemoveAndIsEmpty");
    final LegacyRandomSeeder randomSeeder = createRandomSeeder(seedGenerator);
    checkAddRemoveAndIsEmpty(prng, randomSeeder, randomSeeder::add);
  }
}
