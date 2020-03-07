package io.github.pr0methean.betterrandom.util;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An {@link AtomicReference} that, when serialized, replaces its value with null if it is not
 * {@link Serializable}.
 * @param <T>
 */
public class TransientAtomicReference<T> extends AtomicReference<T> {

  private static final long serialVersionUID = -978042598422633701L;

  /**
   * Creates an instance with the given initial value.
   *
   * @param initialValue the initial value
   */
  public TransientAtomicReference(T initialValue) {
    super(initialValue);
  }

  /**
   * Called by ObjectOutputStream to replace this with the version that is written out.
   *
   * @return a copy that has this instance's value if Serializable, and null otherwise
   */
  protected Object writeReplace() {
    T value = get();
    return new TransientAtomicReference<>(value instanceof Serializable ? value : null);
  }
}
