package io.github.pr0methean.betterrandom.util;

/**
 * Collection of arithmetic methods that treat byte arrays as large fixed-precision integers. Each
 * byte is treated as unsigned, and the array is in {@link java.nio.ByteOrder#BIG_ENDIAN}.
 */
@SuppressWarnings("AccessStaticViaInstance")
public enum Byte16ArrayArithmetic {
  ;

  public static final byte[] ZERO = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
  public static final byte[] ONE = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
  private static final ThreadLocal<byte[]> multiplicationAccumulator = makeByteArrayThreadLocal();
  private static final ThreadLocal<byte[]> multiplicationStep = makeByteArrayThreadLocal();

  /**
   * {@code counter += delta}
   * @param counter the variable-sized input and the result
   * @param delta the long-sized input
   */
  public static void addInto(byte[] counter, long delta) {
    final byte[] addendDigits = new byte[counter.length];
    System.arraycopy(BinaryUtils.convertLongToBytes(delta), 0, addendDigits,
        counter.length - Long.BYTES, Long.BYTES);
    if (delta < 0) {
      // Sign extend
      for (int i = 0; i < (counter.length - Long.BYTES); i++) {
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
    for (int i = counter.length - 1; i >= 0; i--) {
      final byte oldCounter = counter[i];
      counter[i] += delta[i] + (carry ? 1 : 0);
      carry = ((counter[i] & 0xFF) < (oldCounter & 0xFF)) || (carry && (counter[i] == oldCounter));
    }
  }

  /**
   * {@code counter *= multiplier}
   * @param counter the first input and the result
   * @param multiplier the second input
   */
  @SuppressWarnings("NumericCastThatLosesPrecision") public static void multiplyInto(byte[] counter,
      byte[] multiplier) {
    byte[] multiplicationAccumulator =
        copyInto(Byte16ArrayArithmetic.multiplicationAccumulator, ZERO);
    byte[] multiplicationStep = copyInto(Byte16ArrayArithmetic.multiplicationStep, ZERO);
    for (int multiplierDigit = 0; multiplierDigit < multiplier.length; multiplierDigit++) {
      for (int counterDigit = counter.length - multiplierDigit - 1; counterDigit < counter.length; counterDigit++) {
        int destDigit = multiplierDigit + counterDigit - counter.length + 1;
        System.arraycopy(ZERO, 0, multiplicationStep, 0, multiplicationStep.length);
        // Signed multiplication gives same result as unsigned, in the last 2 bytes
        int stepValue = (multiplier[multiplierDigit] & 0xFF) * (counter[counterDigit] & 0xFF);
        multiplicationStep[destDigit] = (byte) stepValue; // lower 8 bits
        if (destDigit > 0) {
          multiplicationStep[destDigit - 1] = (byte) (stepValue >> 8); // upper 8 bits
        }
        addInto(multiplicationAccumulator, multiplicationStep);
      }
    }
    System.arraycopy(multiplicationAccumulator, 0, counter, 0, multiplicationAccumulator.length);
  }

  /**
   * {@code shifted >>>= bits}
   * From <a href="https://github.com/patrickfav/bytes-java/blob/743a6ab60649e6ce7ec972412bdcb42010a46077/src/main/java/at/favre/lib/bytes/Util.java#L395">this source</a>.
   * @param shifted the array input and the result
   * @param bits how many bits to shift by
   * @author Patrick Favre-Bulle
   */
  public static void unsignedShiftRight(byte[] shifted, int bits) {
    if (bits != 0) {
      if (bits > 0) {
        final int shiftMod = bits % 8;
        final byte carryMask = (byte) (0xFF << (8 - shiftMod));
        final int offsetBytes = (bits / 8);

        int sourceIndex;
        for (int i = shifted.length - 1; i >= 0; i--) {
          sourceIndex = i - offsetBytes;
          if (sourceIndex < 0 || sourceIndex >= shifted.length) {
            shifted[i] = 0;
          } else {
            byte src = shifted[sourceIndex];
            byte dst = (byte) ((0xff & src) >>> shiftMod);
            if (sourceIndex - 1 >= 0) {
              dst |= shifted[sourceIndex - 1] << (8 - shiftMod) & carryMask;
            }
            shifted[i] = dst;
          }
        }
      } else {
        bits = -bits;
        final int shiftMod = bits % 8;
        final byte carryMask = (byte) ((1 << shiftMod) - 1);
        final int offsetBytes = (bits / 8);

        int sourceIndex;
        for (int i = 0; i < shifted.length; i++) {
            sourceIndex = i + offsetBytes;
            if (sourceIndex >= shifted.length) {
                shifted[i] = 0;
            } else {
                byte src = shifted[sourceIndex];
                byte dst = (byte) (src << shiftMod);
                if (sourceIndex + 1 < shifted.length) {
                    dst |= shifted[sourceIndex + 1] >>> (8 - shiftMod) & carryMask;
                }
                shifted[i] = dst;
            }
        }
      }
    }
  }

  /**
   * {@code result ^= operand}. Inputs must be the same length.
   * @param result the first input and the result
   * @param operand
   */
  public static void xorInto(byte[] result, byte[] operand) {
    for (int i = 0; i < result.length; i++) {
      result[i] ^= operand[i];
    }
  }

  /**
   * {@code result |= operand}. Inputs must be the same length.
   * @param result the first input and the result
   * @param operand
   */
  public static void orInto(byte[] result, byte[] operand) {
    for (int i = 0; i < result.length; i++) {
      result[i] |= operand[i];
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
    return ThreadLocal.withInitial(ZERO::clone);
  }

  public static byte[] copyInto(ThreadLocal<byte[]> dest, byte[] src) {
    byte[] out = dest.get();
    System.arraycopy(src, 0, out, 0, src.length);
    return out;
  }
}
