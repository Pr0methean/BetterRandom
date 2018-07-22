package io.github.pr0methean.betterrandom.util;

import java.nio.ByteBuffer;

/**
 * Collection of arithmetic methods that treat byte arrays as large fixed-precision integers. Each
 * byte is treated as unsigned, and the array is in {@link java.nio.ByteOrder#BIG_ENDIAN}.
 */
@SuppressWarnings("AccessStaticViaInstance")
public enum Byte16ArrayArithmetic {
  ;

  public static final byte[] ZERO = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
  public static final byte[] ONE = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
  private static final ThreadLocal<ByteBuffer> multiplicationAccumulator = makeByteArrayThreadLocal();
  private static final ThreadLocal<ByteBuffer> multiplicationStep = makeByteArrayThreadLocal();

  /**
   * {@code counter += delta}
   * @param counter the variable-sized input and the result
   * @param delta the long-sized input
   */
  public static void addInto(ByteBuffer counter, long delta) {
    if (delta == 0) {
      return;
    }
    final long oldLeast = counter.getLong(1);
    long newLeast = oldLeast + delta;
    counter.putLong(1, newLeast);
    if (newLeast <= oldLeast && delta > 0) {
      counter.putLong(0, counter.getLong(0) + 1);
    } else if (newLeast >= oldLeast && delta < 0) {
      counter.putLong(0, counter.getLong(0) - 1);
    }
  }

  /**
   * {@code counter += delta}. Inputs must be the same length.
   * @param counter the first input and the result
   * @param delta the second input
   */
  public static void addInto(ByteBuffer counter, ByteBuffer delta) {
    long most = delta.getLong(0);
    long least = delta.getLong(1);
    if (most >= 0 && least < 0) {
      most--;
    } else if (most < 0 && least >= 0) {
      most++;
    }
    addInto(counter, least);
    counter.putLong(0, counter.getLong(0) + most);
  }

  /**
   * {@code counter *= multiplier}
   * @param counterB the first input and the result
   * @param multiplierB the second input
   */
  @SuppressWarnings("NumericCastThatLosesPrecision") public static void multiplyInto(
      ByteBuffer counterB, ByteBuffer multiplierB) {
    byte[] counter = counterB.array();
    byte[] multiplier = multiplierB.array();
    ByteBuffer multiplicationAccumulator =
        copyInto(Byte16ArrayArithmetic.multiplicationAccumulator, ZERO);
    final ByteBuffer multiplicationStepB = copyInto(Byte16ArrayArithmetic.multiplicationStep, ZERO);
    byte[] multiplicationStep = multiplicationStepB.array();
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
        addInto(multiplicationAccumulator, multiplicationStepB);
      }
    }
    System.arraycopy(multiplicationAccumulator.array(), 0, counter, 0,
        multiplicationAccumulator.array().length);
  }

  /**
   * {@code shifted >>>= bits}
   * From <a href="https://github.com/patrickfav/bytes-java/blob/743a6ab60649e6ce7ec972412bdcb42010a46077/src/main/java/at/favre/lib/bytes/Util.java#L395">this source</a>.
   * @param shiftedB the array input and the result
   * @param bits how many bits to shift by
   * @author Patrick Favre-Bulle
   */
  public static void unsignedShiftRight(ByteBuffer shiftedB, int bits) {
    byte[] shifted = shiftedB.array();
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
   * @param resultB the first input and the result
   * @param operandB the second input
   */
  public static void xorInto(ByteBuffer resultB, ByteBuffer operandB) {
    byte[] result = resultB.array();
    byte[] operand = operandB.array();
    for (int i = 0; i < result.length; i++) {
      result[i] ^= operand[i];
    }
  }

  /**
   * {@code result |= operand}. Inputs must be the same length.
   * @param resultB the first input and the result
   * @param operandB the second input
   */
  public static void orInto(ByteBuffer resultB, ByteBuffer operandB) {
    byte[] result = resultB.array();
    byte[] operand = operandB.array();
    for (int i = 0; i < result.length; i++) {
      result[i] |= operand[i];
    }
  }

  /**
   * {@code shifted <<= bits}
   * @param shifted the array input and the result
   * @param bits how many bits to shift by
   */
  public static void unsignedShiftLeft(ByteBuffer shifted, int bits) {
    unsignedShiftRight(shifted, -bits);
  }

  public static ThreadLocal<ByteBuffer> makeByteArrayThreadLocal() {
    return ThreadLocal.withInitial(() -> ByteBuffer.wrap(ZERO.clone()));
  }

  public static ByteBuffer copyInto(ThreadLocal<ByteBuffer> dest, byte[] src) {
    ByteBuffer out = dest.get();
    System.arraycopy(src, 0, out.array(), 0, src.length);
    return out;
  }
}
