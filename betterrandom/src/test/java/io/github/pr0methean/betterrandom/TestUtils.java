package io.github.pr0methean.betterrandom;

/**
 * Utility methods used only for testing, but by both {@link io.github.pr0methean.betterrandom.prng}
 * and {@link io.github.pr0methean.betterrandom.seed}.
 */
public final class TestUtils {

  /** This is a utility class and shouldn't be instantiated. */
  private TestUtils() {
  }

  /**
   * Appveyor doesn't currently use enough IP addresses to get all its random.org usage allowed, so
   * this test won't run there.
   *
   * @return true if we're not running on Appveyor, false if we are.
   */
  @TestingDeficiency
  public static boolean notOnAppveyor() {
    return System.getenv("APPVEYOR") == null;
  }
}
