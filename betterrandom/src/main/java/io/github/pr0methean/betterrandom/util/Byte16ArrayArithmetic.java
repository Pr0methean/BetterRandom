package io.github.pr0methean.betterrandom.util;

import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertBytesToInt;
import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertBytesToLong;
import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertIntToBytes;
import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertLongToBytes;

/**
 * Collection of arithmetic methods that treat {@code byte[16]} arrays as 128-bit unsigned integers.
 */
@SuppressWarnings("AccessStaticViaInstance")
public enum Byte16ArrayArithmetic {
  ;

  private static final int SIZE_BYTES = 16;
  public static final int SIZE_BYTES_MINUS_LONG = SIZE_BYTES - Long.BYTES;
  public static final byte[] ZERO = new byte[SIZE_BYTES];
  public static final byte[] ONE = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
  private static final ThreadLocal<byte[]> multAccum = makeByteArrayThreadLocal();
  private static final ThreadLocal<byte[]> multStep = makeByteArrayThreadLocal();
  private static final ThreadLocal<byte[]> addendDigits = makeByteArrayThreadLocal();

  /**
   * {@code counter += delta}
   * @param counter the variable-sized input and the result
   * @param delta the long-sized input
   * @param signed if true, treat {@code delta} as signed
   */
  public static void addInto(byte[] counter, long delta, boolean signed) {
    byte[] addendDigits = Byte16ArrayArithmetic.addendDigits.get();
    BinaryUtils.convertLongToBytes(delta, addendDigits, SIZE_BYTES_MINUS_LONG);
    final byte signExtend = (byte) ((signed && (delta < 0)) ? -1 : 0);
    for (int i = 0; i < SIZE_BYTES_MINUS_LONG; i++) {
      addendDigits[i] = signExtend;
    }
    addInto(counter, addendDigits);
  }
    
  /**
   * {@code counter += delta}. Inputs must be the same length.
   * @param counter the first input and the result
   * @param delta the second input
   */
  public static void addInto(byte[] counter, byte[] delta) {
    boolean carry = false;
    for (int i = SIZE_BYTES - 1; i >= 0; i--) {
      final int oldCounterUnsigned = counter[i] < 0 ? counter[i] + 256 : counter[i];
      counter[i] += delta[i] + (carry ? 1 : 0);
      final int newCounterUnsigned = counter[i] < 0 ? counter[i] + 256 : counter[i];
      carry = (oldCounterUnsigned > newCounterUnsigned)
          || (carry && (oldCounterUnsigned == newCounterUnsigned));
    }
  }

  /**
   * {@code counter *= multiplier}
   * @param counter the first input and the result
   * @param multiplier the second input
   */
  @SuppressWarnings("NumericCastThatLosesPrecision") public static void multiplyInto(
      byte[] counter, byte[] multiplier) {
    byte[] multAccum = Byte16ArrayArithmetic.multAccum.get();
    System.arraycopy(ZERO, 0, multAccum, 0, SIZE_BYTES);
    final byte[] multStep = Byte16ArrayArithmetic.multStep.get();
    for (int multiplierLimb = 0; multiplierLimb < 4; multiplierLimb++) {
      for (int counterLimb = 3 - multiplierLimb; counterLimb < 4; counterLimb++) {
        int destLimb = multiplierLimb + counterLimb - 3;
        long stepValue = convertBytesToInt(counter, Integer.BYTES * counterLimb) *
            ((long) convertBytesToInt(multiplier, Integer.BYTES * multiplierLimb));
        if (destLimb == 0) {
          convertIntToBytes((int) stepValue, multStep, Integer.BYTES * destLimb);
        } else {
          convertLongToBytes(stepValue, multStep, Integer.BYTES * (destLimb - 1));
        }
        addInto(multAccum, multStep);
      }
    }
    System.arraycopy(multAccum, 0, counter, 0, SIZE_BYTES);
  }

  /**
   * {@code shifted >>>= bits}
   * From <a href="https://github.com/patrickfav/bytes-java/blob/743a6ab60649e6ce7ec972412bdcb42010a46077/src/main/java/at/favre/lib/bytes/Util.java#L395">this source</a>.
   * @param shiftedB the array input and the result
   * @param bits how many bits to shift by
   * @author Patrick Favre-Bulle
   */
  public static void unsignedShiftRight(byte[] shiftedB, int bits) {
    if (bits == 0) {
      return;
    }
    long oldMost = convertBytesToLong(shiftedB);
    long oldLeast = convertBytesToLong(shiftedB, Long.BYTES);
    convertLongToBytes((oldMost >>> bits) | (oldLeast >>> (bits + 64)), shiftedB, 0);
    convertLongToBytes( (oldLeast >>> bits) | (oldMost >>> (bits - 64)), shiftedB, Long.BYTES);
  }

  /**
   * {@code result ^= operand}. Inputs must be the same length.
   * @param resultB the first input and the result
   * @param operandB the second input
   */
  public static void xorInto(byte[] resultB, byte[] operandB) {
    for (int i = 0; i < SIZE_BYTES; i++) {
      resultB[i] ^= operandB[i];
    }
  }

  /**
   * {@code result |= operand}. Inputs must be the same length.
   * @param resultB the first input and the result
   * @param operandB the second input
   */
  public static void orInto(byte[] resultB, byte[] operandB) {
    for (int i = 0; i < SIZE_BYTES; i++) {
      resultB[i] |= operandB[i];
    }
  }

  /**
   * {@code shifted <<= bits}
   * @param shifted the array input and the result
   * @param bits how many bits to shift by
   */
  public static void unsignedShiftLeft(byte[] shifted, int bits) {
    unsignedShiftRight(shifted, -bits);
  }

  public static ThreadLocal<byte[]> makeByteArrayThreadLocal() {
    return ThreadLocal.withInitial(() -> new byte[SIZE_BYTES]);
  }

}
