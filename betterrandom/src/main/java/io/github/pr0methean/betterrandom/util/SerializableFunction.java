package io.github.pr0methean.betterrandom.util;

import java.io.Serializable;
import java.util.function.Function;

/**
 * Serializable {@link Function}. Needed due to limitations of intersection types in Java.
 *
 * @param <T> the parameter type
 * @param <R> the return type
 */
public interface SerializableFunction<T, R> extends Function<T, R>, Serializable {
}
