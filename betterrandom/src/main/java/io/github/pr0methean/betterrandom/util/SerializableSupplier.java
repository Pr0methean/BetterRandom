package io.github.pr0methean.betterrandom.util;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * Serializable {@link Supplier}. Needed due to limitations of intersection types in Java.
 *
 * @param <T> the supplied type
 */
@FunctionalInterface
public interface SerializableSupplier<T> extends Supplier<T>, Serializable { }
