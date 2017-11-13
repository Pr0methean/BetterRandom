package io.github.pr0methean.betterrandom.util;

import java.io.Serializable;
import java8.util.function.Function;

/**
 * {@link Function} that is {@link Serializable}. In Java 8, this can be replaced with a
 * lambda cast to an intersection type.
 * @param <I> the parameter type
 * @param <O> the return type
 */
public interface SerializableFunction<I, O> extends Function<I, O>, Serializable {

}
