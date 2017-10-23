package io.github.pr0methean.betterrandom.util;

import java.io.Serializable;
import java8.util.function.Supplier;

/**
 * {@link Supplier} that is {@link Serializable}. In Java 8, this can be replaced with a
 * lambda cast to an intersection type.
 * @param <T> the type of object supplied.
 */
public interface SerializableSupplier<T> extends Supplier<T>, Serializable {
}
