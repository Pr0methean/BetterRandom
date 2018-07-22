package io.github.pr0methean.betterrandom.util;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.Arrays;
import org.testng.annotations.Test;

@SuppressWarnings("SpellCheckingInspection")
public class Byte16ArrayArithmeticTest {

  private static ByteBuffer longsToByteBuffer(long most, long least) {
    ByteBuffer out = ByteBuffer.wrap(new byte[16]);
    out.putLong(0, most);
    out.putLong(1, least);
    return out;
  }

  private static ByteBuffer cloneByteBuffer(ByteBuffer original) {
    return ByteBuffer.wrap(original.array().clone());
  }

  private static final ByteBuffer OPERAND1 =
      longsToByteBuffer(0x79EC_964A_738B_2EBAL, 0x38FD_07E7_D607_B6EDL);

  private static final ByteBuffer OPERAND2 =
      longsToByteBuffer(0xC26B_1E45_A661_872BL, 0x631B_C188_2D24_D6E9L);

  private static void assertByteBufferEqualsLongs(
      ByteBuffer actual, long expectedMost, long expectedLeast) {
    String message = String.format("Expected %016X%016X, got %s",
        expectedMost, expectedLeast, BinaryUtils.convertBytesToHexString(actual.array()));
    assertEquals(actual.getLong(0), expectedMost, message);
    assertEquals(actual.getLong(1), expectedLeast, message);
  }

  @Test public void testAddIntoBb() {
    ByteBuffer result = cloneByteBuffer(OPERAND1);
    Byte16ArrayArithmetic.addInto(result, OPERAND2);
    assertByteBufferEqualsLongs(result, 0x3C57_B490_19EC_B5E5L, 0x9C18_C970_032C_8DD6L);
  }

  @Test public void testAddIntoLongSigned() {
    ByteBuffer result = cloneByteBuffer(OPERAND1);
    Byte16ArrayArithmetic.addInto(result, 0x631bc1882d24d6e9L, true);
    assertByteBufferEqualsLongs(result, 0x79EC_964A_738B_2EBAL, 0x9C18_C970_032C_8DD6L);
    Byte16ArrayArithmetic.addInto(result, -0x631bc1882d24d6e9L, true);
    assertByteBufferEqualsLongs(result, 0x79EC_964A_738B_2EBAL, 0x38FD_07E7_D607_B6EDL);
  }

  @Test public void testAddIntoLongUnsigned() {
    ByteBuffer result = cloneByteBuffer(OPERAND1);
    Byte16ArrayArithmetic.addInto(result, 0x631bc1882d24d6e9L, false);
    assertByteBufferEqualsLongs(result, 0x79EC_964A_738B_2EBAL, 0x9C18_C970_032C_8DD6L);
    Byte16ArrayArithmetic.addInto(result, -0x631bc1882d24d6e9L, false);
    assertByteBufferEqualsLongs(result, 0x79EC_964A_738B_2EBBL, 0x38FD_07E7_D607_B6EDL);

  }

  @Test public void testMultiplyInto() {
    ByteBuffer result = cloneByteBuffer(OPERAND1);
    Byte16ArrayArithmetic.multiplyInto(result, OPERAND2);
    assertByteBufferEqualsLongs(result, 0x3477_9500_5A1B_EFFCL, 0xB722_4E11_A243_9BB5L);
  }

  @Test public void testUnsignedShiftRight() {
    ByteBuffer result = cloneByteBuffer(OPERAND1);
    Byte16ArrayArithmetic.unsignedShiftRight(result, 12);
    assertByteBufferEqualsLongs(result, 0x0007_9EC9_64A7_38B2L, 0xEBA3_8FD0_7E7D_607BL);
  }

  @Test public void testUnsignedShiftRightOffEdge() {
    ByteBuffer result = cloneByteBuffer(OPERAND1);
    Byte16ArrayArithmetic.unsignedShiftRight(result, 128);
    assertByteBufferEqualsLongs(result, 0, 0);
  }

  @Test public void testUnsignedShiftLeft() {
    ByteBuffer result = cloneByteBuffer(OPERAND1);
    Byte16ArrayArithmetic.unsignedShiftLeft(result, 12);
    assertByteBufferEqualsLongs(result, 0xC964_A738_B2EB_A38FL, 0xD07E_7D60_7B6E_D000L);
  }

  @Test public void testUnsignedShiftLeftOffEdge() {
    ByteBuffer result = cloneByteBuffer(OPERAND1);
    Byte16ArrayArithmetic.unsignedShiftLeft(result, 128);
    assertByteBufferEqualsLongs(result, 0, 0);
  }

  @Test public void testXorInto() {
    ByteBuffer result = cloneByteBuffer(OPERAND1);
    Byte16ArrayArithmetic.xorInto(result, OPERAND2);
    assertByteBufferEqualsLongs(result, 0xBB87_880F_D5EA_A991L, 0x5BE6_C66F_FB23_6004L);
  }

  @Test public void testCopyInto() {
    ThreadLocal<ByteBuffer> threadLocal = Byte16ArrayArithmetic.makeByteArrayThreadLocal();
    final byte[] operand1Array = OPERAND1.array();
    byte[] copyOfOperand1 = Byte16ArrayArithmetic.copyInto(threadLocal, operand1Array).array();
    assertTrue(Arrays.equals(copyOfOperand1, operand1Array));
    assertNotSame(copyOfOperand1, operand1Array);
  }
}