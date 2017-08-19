package betterrandom.util;

import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * A {@link WeakReference}&lt;T&gt; that compares the object it refers to when implementing
 * {@link Object#equals(Object)} and {@link Object#hashCode()}.
 *
 * @param <T> The type of object referred to.
 */
public class WeakReferenceWithEquals<T> extends WeakReference<T> {

  private final int hashCode;

  public WeakReferenceWithEquals(T target) {
    super(target);
    hashCode = target.hashCode();
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public boolean equals(Object o) {
    return (this == o) || (o instanceof WeakReferenceWithEquals
        && Objects.equals(((WeakReferenceWithEquals) o).get(), get()));
  }
}
