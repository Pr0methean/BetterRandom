package io.github.pr0methean.betterrandom;

public interface SeekableRandom extends RepeatableRandom {

  /**
   * Advances the generator forward {@code delta} steps, but does so in logarithmic time.
   *
   * @param delta the number of steps to advance; can be negative
   */
  void advance(long delta);
}
