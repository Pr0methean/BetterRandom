package io.github.pr0methean.betterrandom;

/**
 * A {@link RepeatableRandom} that can skip backward or forward within its sequence of output.
 */
public interface SeekableRandom extends RepeatableRandom {

  /**
   * Advances the generator forward {@code delta} steps, but does so in logarithmic time.
   *
   * @param delta the number of steps to advance; can be negative
   */
  void advance(long delta);
}
