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
    Random copy1AsRandom = createRng();
    SeekableRandom copy1 = (SeekableRandom) copy1AsRandom;
    Random copy2AsRandom = createRng(copy1.getSeed());
    SeekableRandom copy2 = (SeekableRandom) copy2AsRandom;
    for (int i = 0; i < ITERATIONS; i++) {
      copy1AsRandom.nextInt();
    }
    copy2.advance(ITERATIONS);
    RandomTestUtils.testEquivalence(copy1AsRandom, copy2AsRandom, ITERATIONS);
  }

  @Test public void testAdvanceZero() {
    Random copy1AsRandom = createRng();
    SeekableRandom copy1 = (SeekableRandom) copy1AsRandom;
    Random copy2AsRandom = createRng(copy1.getSeed());
    SeekableRandom copy2 = (SeekableRandom) copy2AsRandom;
    copy2.advance(0);
    RandomTestUtils.testEquivalence(copy1AsRandom, copy2AsRandom, ITERATIONS);
  }

  @Test public void testAdvanceBackward() {
    Random copy1AsRandom = createRng();
    SeekableRandom copy1 = (SeekableRandom) copy1AsRandom;
    Random copy2AsRandom = createRng(copy1.getSeed());
    SeekableRandom copy2 = (SeekableRandom) copy2AsRandom;
    for (int i = 0; i < ITERATIONS; i++) {
      copy1AsRandom.nextInt();
    }
    copy1.advance(-ITERATIONS);
    RandomTestUtils.testEquivalence(copy1AsRandom, copy2AsRandom, ITERATIONS);
  }
}
