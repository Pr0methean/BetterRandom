package io.github.pr0methean.betterrandom

import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import java.util.Arrays
import java.util.function.Consumer

/**
 * Utility methods used only for testing, but by both [io.github.pr0methean.betterrandom.prng]
 * and [io.github.pr0methean.betterrandom.seed].
 */
enum class TestUtils {
    ;

    companion object {

        /**
         * Reflectively calls all public constructors, or all public and protected constructors, of the
         * given class with the given parameters. Passes each constructed instance to a consumer.
         * @param <T> `clazz` as a type.
         * @param clazz The class whose constructors are to be tested.
         * @param includeProtected Whether to test protected constructors
         * @param params A map of parameter types to values.
         * @param test The consumer to pass the instances to.
        </T> */
        fun <T> testConstructors(
                clazz: Class<out T>, includeProtected: Boolean,
                params: Map<Class<*>, Any>, test: Consumer<in T>) {
            for (constructor in clazz.declaredConstructors) {
                val modifiers = constructor.modifiers
                if (Modifier.isPublic(modifiers) || includeProtected && Modifier.isProtected(modifiers)) {
                    constructor.isAccessible = true
                    val nParams = constructor.parameterCount
                    val parameterTypes = constructor.parameterTypes
                    val constructorParams = arrayOfNulls<Any>(nParams)
                    try {
                        for (i in 0 until nParams) {
                            constructorParams[i] = params[parameterTypes[i]]
                        }
                        test.accept(constructor.newInstance(*constructorParams) as T)
                    } catch (e: IllegalAccessException) {
                        throw AssertionError(String
                                .format("Failed to call%n%s%nwith parameters%n%s", constructor.toGenericString(),
                                        Arrays.toString(constructorParams)), e)
                    } catch (e: InstantiationException) {
                        throw AssertionError(String.format("Failed to call%n%s%nwith parameters%n%s", constructor.toGenericString(), Arrays.toString(constructorParams)), e)
                    } catch (e: InvocationTargetException) {
                        throw AssertionError(String.format("Failed to call%n%s%nwith parameters%n%s", constructor.toGenericString(), Arrays.toString(constructorParams)), e)
                    } catch (e: IllegalArgumentException) {
                        throw AssertionError(String.format("Failed to call%n%s%nwith parameters%n%s", constructor.toGenericString(), Arrays.toString(constructorParams)), e)
                    }

                }
            }
        }

        /**
         * Appveyor doesn't seem to be allowed any random.org usage at all, even with a valid API key.
         * @return true if we're running on Appveyor, false otherwise
         */
        val isAppveyor: Boolean
            @TestingDeficiency get() = System.getenv("APPVEYOR") != null

        fun assertLessOrEqual(actual: Long, expected: Long) {
            if (actual > expected) {
                throw AssertionError(
                        String.format("Expected no more than %d but found %d", expected, actual))
            }
        }

        fun assertLessOrEqual(actual: Double, expected: Double) {
            if (actual > expected) {
                throw AssertionError(
                        String.format("Expected no more than %f but found %f", expected, actual))
            }
        }

        fun assertGreaterOrEqual(actual: Long, expected: Long) {
            if (actual < expected) {
                throw AssertionError(
                        String.format("Expected at least %d but found %d", expected, actual))
            }
        }

        fun assertGreaterOrEqual(actual: Double, expected: Double) {
            if (actual < expected) {
                throw AssertionError(
                        String.format("Expected at least %f but found %f", expected, actual))
            }
        }

        fun assertLess(actual: Double, expected: Double) {
            if (actual >= expected) {
                throw AssertionError(
                        String.format("Expected less than %f but found %f", expected, actual))
            }
        }
    }
}
