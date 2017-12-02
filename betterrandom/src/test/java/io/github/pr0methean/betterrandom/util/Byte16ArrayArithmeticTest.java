package io.github.pr0methean.betterrandom.util;

import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertBytesToHexString;
import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertHexStringToBytes;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

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

  @Test public void testAddInto() throws Exception {
    byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.addInto(result, OPERAND2);
    assertArrayEqualsHex(result, "3C57B49019ECB5E59C18C970032C8DD6");
  }

  @Test public void testAddInto1() throws Exception {
    byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.addInto(result, 0x631bc1882d24d6e9L);
    assertArrayEqualsHex(result, "79EC964A738B2EBA9C18C970032C8DD6");
  }

  @Test public void testMultiplyInto() throws Exception {
    byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.multiplyInto(result, OPERAND2);
    assertArrayEqualsHex(result, "347795005A1BEFFCB7224E11A2439BB5");
  }

  @Test public void testUnsignedShiftRight() throws Exception {
    byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.unsignedShiftRight(result, 12);
    assertArrayEqualsHex(result, "00079EC964A738B2EBA38FD07E7D607B");
  }

  @Test public void testUnsignedShiftRightOffEdge() throws Exception {
    byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.unsignedShiftRight(result, 128);
    assertTrue(Arrays.equals(result, Byte16ArrayArithmetic.ZERO));
  }

  @Test public void testUnsignedShiftLeft() throws Exception {
    byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.unsignedShiftLeft(result, 12);
    assertArrayEqualsHex(result, "C964A738B2EBA38FD07E7D607B6ED000");
  }

  @Test public void testUnsignedShiftLeftOffEdge() throws Exception {
    byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.unsignedShiftLeft(result, 128);
    assertTrue(Arrays.equals(result, Byte16ArrayArithmetic.ZERO));
  }

  @Test public void testXorInto() throws Exception {
    byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.xorInto(result, OPERAND2);
    assertArrayEqualsHex(result, "BB87880FD5EAA9915BE6C66FFB236004");
  }

  @Test public void testCopyInto() throws Exception {
    ThreadLocal<byte[]> threadLocal = Byte16ArrayArithmetic.makeByteArrayThreadLocal();
    assertTrue(Arrays.equals(Byte16ArrayArithmetic.copyInto(threadLocal, OPERAND1), OPERAND1));
  }
}