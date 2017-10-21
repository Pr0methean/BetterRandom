package io.github.pr0methean.betterrandom.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public enum CloneViaSerialization {
  /* Utility class with no instances */;

  /**
   * Clones an object by serializing and deserializing it.
   * @param object The object to clone.
   * @param <T> The type of {@code object}.
   * @return A clone of {@code object}.
   */
  @SuppressWarnings("unchecked") public static <T extends Serializable> T clone(final T object) {
    try (ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutStream = new ObjectOutputStream(byteOutStream)) {
      objectOutStream.writeObject(object);
      final byte[] serialCopy = byteOutStream.toByteArray();
      // Read the object back-in.
      try (ObjectInputStream objectInStream = new ObjectInputStream(
          new ByteArrayInputStream(serialCopy))) {
        return (T) (objectInStream.readObject());
      }
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
