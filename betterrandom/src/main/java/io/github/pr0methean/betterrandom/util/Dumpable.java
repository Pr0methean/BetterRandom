package io.github.pr0methean.betterrandom.util;

/**
 * Object that can be dumped (written for debugging purposes to a more detailed string
 * representation than what {@link Object#toString()} returns.
 *
 * @author Chris Hennick
 */
public interface Dumpable {

  /**
   * @return a {@link String} representing the state of this object for debugging purposes, in more
   *     detail than {@link Object#toString()} returns.
   */
  @EntryPoint
  String dump();
}
