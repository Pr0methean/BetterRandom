package io.github.pr0methean.betterrandom;

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
   *
   * @param object The object to clone.
   * @param <T> The type of {@code object}.
   * @return A clone of {@code object}.
   */
  @SuppressWarnings("unchecked") public static <T extends Serializable> T clone(final T object) {
    return fromByteArray(toByteArray(object));
  }

  @SuppressWarnings("unchecked")
  public static <T extends Serializable> T fromByteArray(final byte[] serialCopy) {
    try (final ObjectInputStream objectInStream = new ObjectInputStream(
        new ByteArrayInputStream(serialCopy))) {
      return (T) (objectInStream.readObject());
    } catch (final IOException | ClassNotFoundException e) {
      throw new AssertionError(e);
    }
  }

  public static <T extends Serializable> byte[] toByteArray(final T object) {
    try (final ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        final ObjectOutputStream objectOutStream = new ObjectOutputStream(byteOutStream)) {
      objectOutStream.writeObject(object);
      return byteOutStream.toByteArray();
    } catch (final IOException e) {
      throw new AssertionError(e);
    }
  }
}
