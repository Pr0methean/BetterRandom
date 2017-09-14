package io.github.pr0methean.betterrandom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/** Helper methods used for testing of multiple packages. */
public final class TestUtil {
  /** Utility class that should not be instantiated. */
  private TestUtil() {}

  @SuppressWarnings("unchecked")
  public static <T extends Serializable> T serializeAndDeserialize(final T object) {
    try (
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
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

  public static class MockException extends RuntimeException {}
}
