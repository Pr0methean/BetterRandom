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
    byte[] serialCopy;
    serialCopy = toByteArray(object);
    // Read the object back-in.
    return fromByteArray(serialCopy);
  }

  public static <T extends Serializable> T fromByteArray(byte[] serialCopy) {
    try (ObjectInputStream objectInStream = new ObjectInputStream(
        new ByteArrayInputStream(serialCopy))) {
      return (T) (objectInStream.readObject());
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T extends Serializable> byte[] toByteArray(T object) {
    byte[] serialCopy;
    try (ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutStream = new ObjectOutputStream(byteOutStream)) {
      objectOutStream.writeObject(object);
      serialCopy = byteOutStream.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return serialCopy;
  }
}
