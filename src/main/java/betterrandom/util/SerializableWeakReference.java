package betterrandom.util;

import java.io.Serializable;

public class SerializableWeakReference<T extends Serializable> extends WeakReferenceWithEquals<T> {

  public SerializableWeakReference(T target) {
    super(target);
  }
}
