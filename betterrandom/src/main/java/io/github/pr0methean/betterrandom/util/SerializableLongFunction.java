package io.github.pr0methean.betterrandom.util;

import java.io.Serializable;
import java.util.function.LongFunction;

/**
 * Serializable {@link LongFunction}. Needed due to limitations of intersection types in Java.
 *
 * @param <R> the return type
 */
public interface SerializableLongFunction<R> extends LongFunction<R>, Serializable { }
