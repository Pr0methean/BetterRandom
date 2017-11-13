package io.github.pr0methean.betterrandom;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java8.util.function.Consumer;

/**
 * Utility methods used only for testing, but by both {@link io.github.pr0methean.betterrandom.prng}
 * and {@link io.github.pr0methean.betterrandom.seed}.
 */
public enum TestUtils {
  ;

  /**
   * Reflectively calls all public constructors of the given class with the given parameters, and
   * passes each constructed instance to a consumer.
   * @param clazz The class whose constructors are to be tested.
   * @param params A map of parameter types to values.
   * @param test The consumer to pass the instances to.
   * @param <T> {@code clazz} as a type.
   */
  @SuppressWarnings("ObjectAllocationInLoop") public static <T> void testAllPublicConstructors(
      final Class<? extends T> clazz, final Map<Class<?>, Object> params, final Consumer<? super T> test) {
    for (final Constructor<?> constructor : clazz.getDeclaredConstructors()) {
      if (Modifier.isPublic(constructor.getModifiers())) {
        final Class<?>[] parameterTypes = constructor.getParameterTypes();
        final int nParams = parameterTypes.length;
        final Object[] constructorParams = new Object[nParams];
        try {
          for (int i = 0; i < nParams; i++) {
            constructorParams[i] = params.get(parameterTypes[i]);
          }
          test.accept((T) constructor.newInstance(constructorParams));
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException | IllegalArgumentException e) {
          throw new AssertionError(String.format("Failed to call%n%s%nwith parameters%n%s",
              constructor.toGenericString(), Arrays.toString(constructorParams)),
              e);
        }
      }
    }
  }

  /**
   * Appveyor and the OSX environment on Travis-CI don't currently use enough IP addresses to get
   * heavy random.org usage allowed, so tests that are sufficiently demanding of random.org won't
   * run on those environments.
   * @return true if we're not running on Appveyor or a Travis-CI OSX instance, false if we are.
   */
  @SuppressWarnings("CallToSystemGetenv") @TestingDeficiency
  public static boolean canRunRandomDotOrgLargeTest() {
    return isNotAppveyor() && !("osx".equals(System.getenv("TRAVIS_OS_NAME")));
  }

  /**
   * Appveyor doesn't seem to be allowed any random.org usage at all, even with a valid API key.
   * @return true if we're not running on Appveyor, false if we are.
   */
  @SuppressWarnings("CallToSystemGetenv") public static boolean isNotAppveyor() {
    return System.getenv("APPVEYOR") == null;
  }

  public static void assertLessOrEqual(final long expected, final long actual) {
    if (actual > expected) {
      throw new AssertionError(
          String.format("Expected no more than %d but found %d", expected, actual));
    }
  }

  public static void assertGreaterOrEqual(final long expected, final long actual) {
    if (actual < expected) {
      throw new AssertionError(
          String.format("Expected at least %d but found %d", expected, actual));
    }
  }

  public static void assertGreaterOrEqual(final double expected, final double actual) {
    if (actual < expected) {
      throw new AssertionError(
          String.format("Expected at least %f but found %f", expected, actual));
    }
  }

  public static void assertLess(final double expected, final double actual) {
    if (actual >= expected) {
      throw new AssertionError(
          String.format("Expected less than %f but found %f", expected, actual));
    }
  }
}
