package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.prng.BaseRandom.entropyOfInt;
import static io.github.pr0methean.betterrandom.prng.BaseRandom.entropyOfLong;
import static org.testng.Assert.assertEquals;

import io.github.pr0methean.betterrandom.util.BinaryUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import org.testng.annotations.Test;

/**
 * Tests for {@link BaseRandom} that are not heritable by tests of subclasses.
 */
@Test(testName = "BaseRandom statics")
public class BaseRandomStaticTest {

  /**
   * This is the serialized form of an instance of a class that has the same name and
   * {@code serialVersionUID} as {@link AesCounterRandom}, but has no fields and does not extend
   * {@link BaseRandom}.
   */
  private static final String AESCOUNTERRANDOM_THAT_DOES_NOT_EXTEND_BASERANDOM =
      "aced000573720037696f2e6769746875622e7072306d65746865616e2e62657474657272616e646f6d2e70726" +
          "e672e416573436f756e74657252616e646f6d42ba5decf4fa6c060200007870";

  @Test(expectedExceptions = InvalidObjectException.class) public void testReadObjectNoData()
      throws IOException, ClassNotFoundException {
    try (final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(
        BinaryUtils.convertHexStringToBytes(AESCOUNTERRANDOM_THAT_DOES_NOT_EXTEND_BASERANDOM)))) {
      ((AesCounterRandom) ois.readObject()).nextInt();
    }

  }

  @Test public void testEntropyOfInt() {
    assertEquals(entropyOfInt(0, 1), 0);
    assertEquals(entropyOfInt(0, 2), 1);
    assertEquals(entropyOfInt(0, 1 << 24), 24);
    assertEquals(entropyOfInt(1 << 22, 1 << 24), 24);
    assertEquals(entropyOfInt(-(1 << 24), 0), 24);
    assertEquals(entropyOfInt(-(1 << 24), 1), 25);
  }

  @Test public void testEntropyOfLong() {
    assertEquals(entropyOfLong(0, 1), 0);
    assertEquals(entropyOfLong(0, 2), 1);
    assertEquals(entropyOfLong(0, 1L << 32), 32);
    assertEquals(entropyOfLong(0, 1L << 42), 42);
    assertEquals(entropyOfLong(0, Long.MAX_VALUE), 63);
    assertEquals(entropyOfLong(Long.MIN_VALUE, Long.MAX_VALUE), 64);
    assertEquals(entropyOfLong(1 << 22, 1L << 42), 42);
    assertEquals(entropyOfLong(-(1L << 42), 0), 42);
    assertEquals(entropyOfLong(-(1L << 42), 1), 43);
  }
}
