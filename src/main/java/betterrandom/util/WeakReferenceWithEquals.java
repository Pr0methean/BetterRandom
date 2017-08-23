package betterrandom.util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * A {@link WeakReference}&lt;T&gt; that compares the object it refers to when implementing {@link
 * Object#equals(Object)} and {@link Object#hashCode()}.
 *
 * @param <T> The type of object referred to.
 */
@SuppressWarnings("SerializableClassWithUnconstructableAncestor")
public class WeakReferenceWithEquals<T extends Serializable> extends WeakReference<T> implements Externalizable {

  private static final long serialVersionUID = -4285013075064416407L;
  private final int hashCode;

  public WeakReferenceWithEquals(T target) {
    super(target);
    hashCode = target.hashCode();
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
  @Override
  public boolean equals(Object o) {
    return (this == o) || (o instanceof WeakReferenceWithEquals
        && Objects.equals(((WeakReferenceWithEquals) o).get(), get()));
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(new SerialWrapper<>(get()));
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    throw new InternalError("SerialWrapper.readResolve should have been called instead!");
  }

  private static class SerialWrapper<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = 3496206565227198499L;
    private final T object;
    private SerialWrapper(T object) {
      this.object = object;
    }

    private WeakReferenceWithEquals<T> readResolve() {
      return new WeakReferenceWithEquals<>(object);
    }
  }
}
