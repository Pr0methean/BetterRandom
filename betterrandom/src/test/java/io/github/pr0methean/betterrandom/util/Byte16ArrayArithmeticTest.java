package io.github.pr0methean.betterrandom.util;

import static io.github.pr0methean.betterrandom.util.Byte16ArrayArithmetic.rotateRightLeast64;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

@SuppressWarnings("SpellCheckingInspection") public class Byte16ArrayArithmeticTest {

  private static final byte[] OPERAND1 =
      longsToByteArray(0x79EC_964A_738B_2EBAL, 0x38FD_07E7_D607_B6EDL);
  private static final byte[] OPERAND2 =
      longsToByteArray(0xC26B_1E45_A661_872BL, 0x631B_C188_2D24_D6E9L);
  private static final byte[] ONES = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
  private static final byte[] MINUS_ONES =
      {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

  private static byte[] longsToByteArray(final long most, final long least) {
    final byte[] out = new byte[16];
    BinaryUtils.convertLongToBytes(most, out, 0);
    BinaryUtils.convertLongToBytes(least, out, Long.BYTES);
    return out;
  }

  private static void assertByteArrayEqualsLongs(final byte[] actual, final long expectedMost,
      final long expectedLeast) {
    final String message = String.format("Expected %016X%016X, got %s", expectedMost, expectedLeast,
        BinaryUtils.convertBytesToHexString(actual));
    assertEquals(BinaryUtils.convertBytesToLong(actual, 0), expectedMost, message);
    assertEquals(BinaryUtils.convertBytesToLong(actual, Long.BYTES), expectedLeast, message);
  }

  @Test public void testAddIntoBb() {
    final byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.addInto(result, OPERAND2);
    assertByteArrayEqualsLongs(result, 0x3C57_B490_19EC_B5E5L, 0x9C18_C970_032C_8DD6L);
  }

  @Test public void testAddIntoLongSigned() {
    final byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.addInto(result, 0x631bc1882d24d6e9L);
    assertByteArrayEqualsLongs(result, 0x79EC_964A_738B_2EBAL, 0x9C18_C970_032C_8DD6L);
    Byte16ArrayArithmetic.addInto(result, -0x631bc1882d24d6e9L);
    assertByteArrayEqualsLongs(result, 0x79EC_964A_738B_2EBAL, 0x38FD_07E7_D607_B6EDL);
  }

  @Test public void testMultiplyIntoMinusOnes() {
    final byte[] result = MINUS_ONES.clone();
    Byte16ArrayArithmetic.multiplyInto(result, MINUS_ONES);
    assertByteArrayEqualsLongs(result, 0, 1);
  }

  @Test public void testMultiplyIntoAndAddInto() {
    final byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.multiplyIntoAndAddInto(result, OPERAND1, OPERAND2);
    assertByteArrayEqualsLongs(result, 0x837a_715f_10d1_6e7fL, 0x046b_bd4d_ecd0_ae52L);
  }

  @Test public void testMultiplyIntoOnes() {
    final byte[] result = ONES.clone();
    Byte16ArrayArithmetic.multiplyInto(result, ONES);
    assertByteArrayEqualsLongs(result, 0x100f_0e0d_0c0b_0a09L, 0x0807_0605_0403_0201L);
  }

  @Test public void testMultiplyInto() {
    // 5c984e32224cdcaac1573c5721c87034347795005a1beffcb7224e11a2439bb5
    final byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.multiplyInto(result, OPERAND2);
    assertByteArrayEqualsLongs(result, 0x3477_9500_5A1B_EFFCL, 0xB722_4E11_A243_9BB5L);
  }

  @Test public void testUnsignedShiftRight() {
    final byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.unsignedShiftRight(result, 12);
    assertByteArrayEqualsLongs(result, 0x0007_9EC9_64A7_38B2L, 0xEBA3_8FD0_7E7D_607BL);
    assertEquals(Byte16ArrayArithmetic.unsignedShiftRightLeast64(OPERAND1, 12),
        0xEBA3_8FD0_7E7D_607BL);
    Byte16ArrayArithmetic.unsignedShiftRight(result, 68);
    assertByteArrayEqualsLongs(result, 0, 0x0_0007_9EC9_64A7_38BL);
    assertEquals(Byte16ArrayArithmetic.unsignedShiftRightLeast64(OPERAND1, 80),
        0x0_0007_9EC9_64A7_38BL);
  }

  @Test public void testRotateLeft() {
    final byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.rotateRight(result, -12);
    assertByteArrayEqualsLongs(result, 0xC_964A_738B_2EBA_38FL, 0xD_07E7_D607_B6ED_79EL);
    assertEquals(rotateRightLeast64(OPERAND1, -12), 0xD_07E7_D607_B6ED_79EL);
  }

  @Test public void testRotateRightLong() {
    final byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.rotateRight(result, 116);
    assertByteArrayEqualsLongs(result, 0xC_964A_738B_2EBA_38FL, 0xD_07E7_D607_B6ED_79EL);
    assertEquals(rotateRightLeast64(OPERAND1, 116), 0xD_07E7_D607_B6ED_79EL);
  }

  @Test public void testRotateRight() {
    final byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.rotateRight(result, 12);
    assertByteArrayEqualsLongs(result, 0x6ED7_9EC9_64A7_38B2L, 0xEBA3_8FD0_7E7D_607BL);
    assertEquals(rotateRightLeast64(OPERAND1, 12), 0xEBA3_8FD0_7E7D_607BL);
  }

  @Test public void testRotateLeftLong() {
    final byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.rotateRight(result, -116);
    assertByteArrayEqualsLongs(result, 0x6ED7_9EC9_64A7_38B2L, 0xEBA3_8FD0_7E7D_607BL);
    assertEquals(rotateRightLeast64(OPERAND1, -116), 0xEBA3_8FD0_7E7D_607BL);
  }

  @Test public void testUnsignedShiftRightOffEdge() {
    final byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.unsignedShiftRight(result, 128);
    assertByteArrayEqualsLongs(result, 0, 0);
  }

  @Test public void testUnsignedShiftLeft() {
    final byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.unsignedShiftRight(result, -12);
    assertByteArrayEqualsLongs(result, 0xC964_A738_B2EB_A38FL, 0xD07E_7D60_7B6E_D000L);
    assertEquals(Byte16ArrayArithmetic.unsignedShiftRightLeast64(OPERAND1, -12),
        0xD07E_7D60_7B6E_D000L);
    Byte16ArrayArithmetic.unsignedShiftRight(result, -68);
    assertByteArrayEqualsLongs(result, 0x07E_7D60_7B6E_D000_0L, 0);
    assertEquals(Byte16ArrayArithmetic.unsignedShiftRightLeast64(OPERAND1, -80), 0);

  }

  @Test public void testUnsignedShiftLeftOffEdge() {
    final byte[] result = OPERAND1.clone();
    Byte16ArrayArithmetic.unsignedShiftRight(result, -128);
    assertByteArrayEqualsLongs(result, 0, 0);
  }
}