package io.github.pr0methean.betterrandom.util;

import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertBytesToHexString;
import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertHexStringToBytes;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.Arrays;
import org.testng.annotations.Test;

@SuppressWarnings("SpellCheckingInspection")
public class Byte16ArrayArithmeticTest {

  private static final byte[] OPERAND1 =
      convertHexStringToBytes("79EC964A738B2EBA38FD07E7D607B6ED");

  private static final byte[] OPERAND2 =
      convertHexStringToBytes("C26B1E45A661872B631BC1882D24D6E9");

  private static void assertArrayEqualsHex(byte[] array, String hex) {
    assertEquals(convertBytesToHexString(array), hex);
  }

  @Test public void testAddInto() {
    byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.addInto(ByteBuffer.wrap(result), ByteBuffer.wrap(OPERAND2));
    assertArrayEqualsHex(result, "3C57B49019ECB5E59C18C970032C8DD6");
  }

  @Test public void testAddInto1() {
    byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.addInto(ByteBuffer.wrap(result), 0x631bc1882d24d6e9L);
    assertArrayEqualsHex(result, "79EC964A738B2EBA9C18C970032C8DD6");
  }

  @Test public void testMultiplyInto() {
    byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.multiplyInto(ByteBuffer.wrap(result), ByteBuffer.wrap(OPERAND2));
    assertArrayEqualsHex(result, "347795005A1BEFFCB7224E11A2439BB5");
  }

  @Test public void testUnsignedShiftRight() {
    byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.unsignedShiftRight(ByteBuffer.wrap(result), 12);
    assertArrayEqualsHex(result, "00079EC964A738B2EBA38FD07E7D607B");
  }

  @Test public void testUnsignedShiftRightOffEdge() {
    byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.unsignedShiftRight(ByteBuffer.wrap(result), 128);
    assertTrue(Arrays.equals(result, Byte16ArrayArithmetic.ZERO));
  }

  @Test public void testUnsignedShiftLeft() {
    byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.unsignedShiftLeft(ByteBuffer.wrap(result), 12);
    assertArrayEqualsHex(result, "C964A738B2EBA38FD07E7D607B6ED000");
  }

  @Test public void testUnsignedShiftLeftOffEdge() {
    byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.unsignedShiftLeft(ByteBuffer.wrap(result), 128);
    assertTrue(Arrays.equals(result, Byte16ArrayArithmetic.ZERO));
  }

  @Test public void testXorInto() {
    byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.xorInto(ByteBuffer.wrap(result), ByteBuffer.wrap(OPERAND2));
    assertArrayEqualsHex(result, "BB87880FD5EAA9915BE6C66FFB236004");
  }

  @Test public void testCopyInto() {
    ThreadLocal<ByteBuffer> threadLocal = Byte16ArrayArithmetic.makeByteArrayThreadLocal();
    byte[] copyOfOperand1 = Byte16ArrayArithmetic.copyInto(threadLocal, OPERAND1).array();
    assertTrue(Arrays.equals(copyOfOperand1, OPERAND1));
    assertNotSame(copyOfOperand1, OPERAND1);
  }
}