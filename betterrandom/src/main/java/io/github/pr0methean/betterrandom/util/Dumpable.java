package io.github.pr0methean.betterrandom.util;

/**
 * Object that can be dumped (written for debugging purposes to a more detailed string
 * representation than what {@link Object#toString()} returns).
 *
 * @author Chris Hennick
 */
public interface Dumpable {

  /**
   * Returns a {@link String} representing the state of this object for debugging purposes,
   * including mutable state that {@link Object#toString()} usually doesn't return.
   *
   * @return a representation of this object and its state.
   */
  @EntryPoint String dump();
}
