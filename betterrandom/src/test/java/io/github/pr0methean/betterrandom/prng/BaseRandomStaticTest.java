package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.prng.BaseRandom.entropyOfInt;
import static io.github.pr0methean.betterrandom.prng.BaseRandom.entropyOfLong;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;

import io.github.pr0methean.betterrandom.TestingDeficiency;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import org.testng.annotations.Test;

/**
 * Tests for {@link BaseRandom} that are not heritable by tests of subclasses.
 */
public class BaseRandomStaticTest {

  private static class Switcheroo implements Serializable {

    private static final long serialVersionUID = 5949778642428995210L;
  }

  private static final String SWITCHEROO_NAME = Switcheroo.class.getName();

  private static class SwitcherooInputStream extends ObjectInputStream {
    public SwitcherooInputStream(InputStream in) throws IOException {
      super(in);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc)
        throws IOException, ClassNotFoundException {
      if (Switcheroo.serialVersionUID == desc
          .getSerialVersionUID()) {
        return AesCounterRandom.class;
      } else {
        return super.resolveClass(desc);
      }
    }
  }

  @TestingDeficiency // FIXME: The switcheroo isn't happening!
  @Test(enabled = false)
  public void testReadObjectNoData() throws IOException, ClassNotFoundException {
    BaseRandom switchedRandom;
    try (
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutStream = new ObjectOutputStream(byteOutStream)) {
      objectOutStream.writeObject(new Switcheroo());
      final byte[] serialCopy = byteOutStream.toByteArray();
      // Read the object back-in.
      try (ObjectInputStream objectInStream = new SwitcherooInputStream(
          new ByteArrayInputStream(serialCopy))) {
        switchedRandom = (BaseRandom) objectInStream.readObject(); // ClassCastException
      }
    }
    switchedRandom.nextInt();
  }

  @Test
  public void testEntropyOfInt() {
    assertEquals(entropyOfInt(0, 1), 0);
    assertEquals(entropyOfInt(0, 2), 1);
    assertEquals(entropyOfInt(0, 1 << 24), 24);
    assertEquals(entropyOfInt(1 << 22, 1 << 24), 24);
    assertEquals(entropyOfInt(-(1 << 24), 0), 24);
    assertEquals(entropyOfInt(-(1 << 24), 1), 25);
  }

  @Test
  public void testEntropyOfLong() {
    assertEquals(entropyOfLong(0, 1), 0);
    assertEquals(entropyOfLong(0, 2), 1);
    assertEquals(entropyOfLong(0, 1L << 42), 42);
    assertEquals(entropyOfLong(1 << 22, 1L << 42), 42);
    assertEquals(entropyOfLong(-(1L << 42), 0), 42);
    assertEquals(entropyOfLong(-(1L << 42), 1), 43);
  }
}
