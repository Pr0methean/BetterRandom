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
   * Appveyor and the OSX environment on Travis-CI don't currently use enough IP addresses to get
   * heavy random.org usage allowed, so tests that are sufficiently demanding of random.org won't
   * run on those environments.
   *
   * @return true if we're not running on Appveyor or a Travis-CI OSX instance, false if we are.
   */
  @SuppressWarnings("CallToSystemGetenv")
  @TestingDeficiency
  public static boolean canRunRandomDotOrgLargeTest() {
    return isNotAppveyor()
        && !("osx".equals(System.getenv("TRAVIS_OS_NAME")));
  }

  /**
   * Appveyor doesn't seem to be allowed any random.org usage at all.
   *
   * @return true if we're not running on Appveyor, false if we are.
   */
  @SuppressWarnings("CallToSystemGetenv")
  public static boolean isNotAppveyor() {
    return System.getenv("APPVEYOR") == null;
  }
}
