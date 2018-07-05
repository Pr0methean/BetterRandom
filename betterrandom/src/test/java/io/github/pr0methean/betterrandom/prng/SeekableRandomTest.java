package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.SeekableRandom;
import java.util.Random;
import org.testng.annotations.Test;

/**
 * Abstract test class for a class that implements {@link SeekableRandom}.
 */
public abstract class SeekableRandomTest extends BaseRandomTest {

  private static final int ITERATIONS = 37;

  @Test public void testAdvanceForward() {
    final Random copy1AsRandom = createRng();
    final SeekableRandom copy1 = (SeekableRandom) copy1AsRandom;
    final Random copy2AsRandom = createRng(copy1.getSeed());
    final SeekableRandom copy2 = (SeekableRandom) copy2AsRandom;
    for (int i = 0; i < ITERATIONS; i++) {
      copy1AsRandom.nextInt();
    }
    copy2.advance(ITERATIONS);
    RandomTestUtils.assertEquivalent(copy1AsRandom, copy2AsRandom, ITERATIONS,
        "Output mismatch after advancing forward");
  }

  @Test public void testAdvanceZero() {
    final Random copy1AsRandom = createRng();
    final SeekableRandom copy1 = (SeekableRandom) copy1AsRandom;
    final Random copy2AsRandom = createRng(copy1.getSeed());
    final SeekableRandom copy2 = (SeekableRandom) copy2AsRandom;
    copy2.advance(0);
    RandomTestUtils.assertEquivalent(copy1AsRandom, copy2AsRandom, ITERATIONS,
        "Output mismatch after advancing by zero");
  }

  @Test public void testAdvanceBackward() {
    final Random copy1AsRandom = createRng();
    final SeekableRandom copy1 = (SeekableRandom) copy1AsRandom;
    final Random copy2AsRandom = createRng(copy1.getSeed());
    for (int i = 0; i < ITERATIONS; i++) {
      copy1AsRandom.nextInt();
    }
    copy1.advance(-ITERATIONS);
    RandomTestUtils.assertEquivalent(copy1AsRandom, copy2AsRandom, ITERATIONS,
        "Output mismatch after advancing backward");
  }
}
