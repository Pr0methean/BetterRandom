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
  public static final byte[] ZERO = new byte[SIZE_BYTES];
  public static final byte[] ONE = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
  private static final ThreadLocal<byte[]> multiplicationAccumulator = makeByteArrayThreadLocal();
  private static final ThreadLocal<byte[]> multiplicationStep = makeByteArrayThreadLocal();
  private static final ThreadLocal<byte[]> addendDigits = makeByteArrayThreadLocal();

  /**
   * {@code counter += delta}
   * @param counter the variable-sized input and the result
   * @param delta the long-sized input
   * @param signed if true, treat {@code delta} as signed
   */
  public static void addInto(byte[] counter, long delta, boolean signed) {
    byte[] addendDigits = Byte16ArrayArithmetic.addendDigits.get();
    System.arraycopy(ZERO, 0, addendDigits, 0, SIZE_BYTES - Long.BYTES);
    BinaryUtils.convertLongToBytes(delta, addendDigits, SIZE_BYTES - Long.BYTES);
    if (signed && (delta < 0)) {
      // Sign extend
      for (int i = 0; i < (SIZE_BYTES - Long.BYTES); i++) {
        addendDigits[i] = -1;
      }
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
    for (int i = SIZE_BYTES - 1; i > 0; i--) {
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
    byte[] multiplicationAccumulator =
        copyInto(Byte16ArrayArithmetic.multiplicationAccumulator, ZERO);
    final byte[] multiplicationStepB = Byte16ArrayArithmetic.multiplicationStep.get();
    for (int multiplierLimb = 0; multiplierLimb < 4; multiplierLimb++) {
      for (int counterLimb = 3 - multiplierLimb; counterLimb < 4; counterLimb++) {
        int destLimb = multiplierLimb + counterLimb - 3;
        long stepValue = convertBytesToInt(counter, Integer.BYTES * counterLimb) *
            ((long) convertBytesToInt(multiplier, Integer.BYTES * multiplierLimb));
        if (destLimb == 0) {
          convertIntToBytes((int) stepValue, multiplicationStepB, Integer.BYTES * destLimb);
        } else {
          convertLongToBytes(stepValue, multiplicationStepB, Integer.BYTES * (destLimb - 1));
        }
        addInto(multiplicationAccumulator, multiplicationStepB);
      }
    }
    System.arraycopy(multiplicationAccumulator, 0, counter, 0, SIZE_BYTES);
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
    return ThreadLocal.withInitial(() -> ZERO.clone());
  }

  public static byte[] copyInto(ThreadLocal<byte[]> dest, byte[] src) {
    byte[] out = dest.get();
    System.arraycopy(src, 0, out, 0, src.length);
    return out;
  }
}
