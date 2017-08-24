package betterrandom.util;

import java.lang.ref.WeakReference;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link WeakReference}&lt;T&gt; that compares the object it refers to when implementing {@link
 * Object#equals(Object)} and {@link Object#hashCode()}.
 *
 * @param <T> The type of object referred to.
 */
public class WeakReferenceWithEquals<T> extends WeakReference<T> {

  private final int hashCode;

  public WeakReferenceWithEquals(T target) {
    super(target);
    if (target == null) {
      hashCode = 0;
    } else {
      hashCode = target.hashCode();
    }
  }

  /**
   * {@inheritDoc} Returns the hash code that the target had when this WeakReferenceWithEquals was
   * created. Will not change, even when this reference is cleared or {@code get().hashCode()}
   * changes.
   */
  @Override
  public int hashCode() {
    return hashCode;
  }

  /**
   * {@inheritDoc} All cleared instances of WeakReferenceWithEquals compare as equal.
   */
  @SuppressWarnings("argument.type.incompatible") // Objects.equals handles nulls
  @Override
  public boolean equals(@Nullable Object o) {
    return (this == o) || (o instanceof WeakReferenceWithEquals<?>
        && Objects.equals(((WeakReferenceWithEquals<?>) o).get(), get()));
  }
}
