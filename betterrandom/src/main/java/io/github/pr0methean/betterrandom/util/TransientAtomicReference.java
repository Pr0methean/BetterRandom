package io.github.pr0methean.betterrandom.util;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An {@link AtomicReference} that, when serialized, replaces its value with null if it is not
 * {@link Serializable}.
 * @param <T>
 */
public class TransientAtomicReference<T> extends AtomicReference<T> {

  public TransientAtomicReference(T initialValue) {
    super(initialValue);
  }

  private Object writeReplace() {
    T value = get();
    return new TransientAtomicReference<>(value instanceof Serializable ? value : null);
  }
}
