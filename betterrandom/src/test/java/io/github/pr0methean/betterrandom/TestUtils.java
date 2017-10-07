package io.github.pr0methean.betterrandom;

import io.github.pr0methean.betterrandom.util.LogPreFormatter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Utility methods used only for testing, but by both {@link io.github.pr0methean.betterrandom.prng}
 * and {@link io.github.pr0methean.betterrandom.seed}.
 */
public enum TestUtils {
  ;

  private static final LogPreFormatter LOG = new LogPreFormatter(TestUtils.class);

  /**
   * Reflectively calls all public constructors of the given class with the given parameters, and
   * passes each constructed instance to a consumer.
   *
   * @param clazz The class whose constructors are to be tested.
   * @param params A map of parameter types to values.
   * @param test The consumer to pass the instances to.
   * @param <T> {@code clazz} as a type.
   */
  public static <T> void testAllPublicConstructors(Class<T> clazz, Map<Class<?>, Object> params,
      Consumer<T> test) {
    for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
      if (Modifier.isPublic(constructor.getModifiers())) {
        try {
          int nParams = constructor.getParameterCount();
          Class[] parameterTypes = constructor.getParameterTypes();
          Object[] constructorParams = new Object[nParams];
          for (int i = 0; i < nParams; i++) {
            constructorParams[i] = params.get(parameterTypes[i]);
          }
          test.accept((T) constructor.newInstance(constructorParams));
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
          throw new AssertionError("Failed to call constructor " + constructor.toGenericString(),
              e);
        }
      }
    }
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
