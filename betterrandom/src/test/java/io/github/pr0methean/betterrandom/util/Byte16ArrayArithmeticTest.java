package io.github.pr0methean.betterrandom.util;

import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertHexStringToBytes;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import org.testng.annotations.Test;

@SuppressWarnings("SpellCheckingInspection")
public class Byte16ArrayArithmeticTest {

  private static final byte[] OPERAND1 =
      convertHexStringToBytes("79ec964a738b2eba38fd07e7d607b6ed");

  private static final byte[] OPERAND2 =
      convertHexStringToBytes("c26b1e45a661872b631bc1882d24d6e9");

  private static void assertArrayEqualsHex(byte[] array, String hex) {
    assertTrue(Arrays.equals(array, convertHexStringToBytes(hex)));
  }

  @Test public void testAddInto() throws Exception {
    byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.addInto(result, OPERAND2);
    assertArrayEqualsHex(result, "3c57b49019ecb5e59c18c970032c8dd6");
  }

  @Test public void testAddInto1() throws Exception {
    byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.addInto(result, 0x631bc1882d24d6e9L);
    assertArrayEqualsHex(result, "79ec964a738b2eba9c18c970032c8dd6");
  }

  @Test public void testMultiplyInto() throws Exception {
    byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.multiplyInto(result, OPERAND2);
    assertArrayEqualsHex(result, "347795005a1beffcb7224e11a2439bb5");
  }

  @Test public void testUnsignedShiftRight() throws Exception {
    byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.unsignedShiftRight(result, 12);
    assertArrayEqualsHex(result, "c964a738b2eba38fd07e7d607b6ed000");
  }

  @Test public void testUnsignedShiftRightOffEdge() throws Exception {
    byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.unsignedShiftRight(result, 128);
    assertTrue(Arrays.equals(result, Byte16ArrayArithmetic.ZERO));
  }

  @Test public void testUnsignedShiftLeft() throws Exception {
    byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.unsignedShiftLeft(result, 12);
    assertArrayEqualsHex(result, "00079ec964a738b2eba38fd07e7d607b");
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