package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.SeekableRandom;
import java.util.Random;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Abstract test class for a class that implements {@link SeekableRandom}.
 */
public abstract class SeekableRandomTest<T extends BaseRandom & SeekableRandom>
    extends BaseRandomTest<T> {

  private static final int ITERATIONS = 10; // because bugs may depend on the initial seed value
  private static final int DELTA = 37;

  @DataProvider(name = "deltas"/*, parallel = true*/) public static Object[][] getDeltas() {
    return new Object[][]{{1}, {2}, {3}, {4}, {5}, {8}, {9}, {16}, {17}, {37}};
  }

  @Test(dataProvider = "deltas") public void testAdvanceForward(int delta) {
    for (int i = 0; i < ITERATIONS; i++) {
      final Random copy1AsRandom = createRng();
      final SeekableRandom copy1 = (SeekableRandom) copy1AsRandom;
      final Random copy2AsRandom = createRng(copy1.getSeed());
      final SeekableRandom copy2 = (SeekableRandom) copy2AsRandom;
      for (int j = 0; j < delta; j++) {
        copy1AsRandom.nextInt();
      }
      copy2.advance(delta);
      RandomTestUtils.assertEquivalent(copy1AsRandom, copy2AsRandom, delta + 8,
          "Output mismatch after advancing forward by " + delta);
    }
  }

  @Test public void testAdvanceZero() {
    final Random copy1AsRandom = createRng();
    final SeekableRandom copy1 = (SeekableRandom) copy1AsRandom;
    final Random copy2AsRandom = createRng(copy1.getSeed());
    final SeekableRandom copy2 = (SeekableRandom) copy2AsRandom;
    copy2.advance(0);
    RandomTestUtils.assertEquivalent(copy1AsRandom, copy2AsRandom, DELTA,
        "Output mismatch after advancing by zero");
  }

  @Test(dataProvider = "deltas") public void testAdvanceBackward(int delta) {
    for (int i = 0; i < ITERATIONS; i++) {
      final Random copy1AsRandom = createRng();
      final SeekableRandom copy1 = (SeekableRandom) copy1AsRandom;
      final Random copy2AsRandom = createRng(copy1.getSeed());
      for (int j = 0; j < delta; j++) {
        copy1AsRandom.nextInt();
      }
      copy1.advance(-delta);
      RandomTestUtils.assertEquivalent(copy1AsRandom, copy2AsRandom, delta + 8,
          "Output mismatch after advancing backward by " + delta);
    }
  }
}
