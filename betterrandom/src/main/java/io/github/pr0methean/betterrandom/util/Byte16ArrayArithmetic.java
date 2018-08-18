package io.github.pr0methean.betterrandom.util;

import java.nio.ByteBuffer;

/**
 * Collection of arithmetic methods that treat {@link ByteBuffer} instances wrapping a
 * {@code byte[16]} array as 128-bit unsigned integers.
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
   * @param signed if true, treat {@code delta} as signed
   */
  public static void addInto(ByteBuffer counter, long delta, boolean signed) {
    if (delta == 0) {
      return;
    }
    final long oldLeast = counter.getLong(1);
    long newLeast = oldLeast + delta;
    counter.putLong(Long.BYTES, newLeast);
    int compare = Long.compareUnsigned(newLeast, oldLeast);
    if (compare < 0 && (delta > 0 || !signed)) {
      counter.putLong(0, counter.getLong(0) + 1);
    } else if (compare > 0 && delta < 0 && signed) {
      counter.putLong(0, counter.getLong(0) - 1);
    }
  }

  /**
   * {@code counter += delta}. Inputs must be the same length.
   * @param counter the first input and the result
   * @param delta the second input
   */
  public static void addInto(ByteBuffer counter, ByteBuffer delta) {
    long least = delta.getLong(Long.BYTES);
    /* if (most >= 0 && least < 0) {
      most--;
    } else if (most < 0 && least >= 0) {
      most++;
    } */
    addInto(counter, least, false);
    counter.putLong(0, counter.getLong(0) + delta.getLong(0));
  }

  /**
   * {@code counter *= multiplier}
   * @param counter the first input and the result
   * @param multiplier the second input
   */
  @SuppressWarnings("NumericCastThatLosesPrecision") public static void multiplyInto(
      ByteBuffer counter, ByteBuffer multiplier) {
    ByteBuffer multiplicationAccumulator =
        copyInto(Byte16ArrayArithmetic.multiplicationAccumulator, ZERO);
    final ByteBuffer multiplicationStepB = Byte16ArrayArithmetic.multiplicationStep.get();
    for (int multiplierLimb = 0; multiplierLimb < 4; multiplierLimb++) {
      for (int counterLimb = 3 - multiplierLimb; counterLimb < 4; counterLimb++) {
        int destLimb = multiplierLimb + counterLimb - 3;
        long stepValue = ((long) multiplier.getInt(Integer.BYTES * multiplierLimb))
            * counter.getInt(Integer.BYTES * counterLimb);
        multiplicationStepB.putInt(Integer.BYTES * destLimb, (int) stepValue);
        if (destLimb > 0) {
          multiplicationStepB.putInt(Integer.BYTES * (destLimb - 1), (int) (stepValue >> Integer.SIZE));
        }
        addInto(multiplicationAccumulator, multiplicationStepB);
      }
    }
    counter.putLong(0, multiplicationAccumulator.getLong(0));
    counter.putLong(Long.BYTES, multiplicationAccumulator.getLong(Long.BYTES));
  }

  /**
   * {@code shifted >>>= bits}
   * From <a href="https://github.com/patrickfav/bytes-java/blob/743a6ab60649e6ce7ec972412bdcb42010a46077/src/main/java/at/favre/lib/bytes/Util.java#L395">this source</a>.
   * @param shiftedB the array input and the result
   * @param bits how many bits to shift by
   * @author Patrick Favre-Bulle
   */
  public static void unsignedShiftRight(ByteBuffer shiftedB, int bits) {
    if (bits == 0) {
      return;
    }
    long oldMost = shiftedB.getLong(0);
    long oldLeast = shiftedB.getLong(Long.BYTES);
    shiftedB.putLong(0, (oldMost >>> bits) | (oldLeast >>> (bits + 64)));
    shiftedB.putLong(Long.BYTES, (oldLeast >>> bits) | (oldMost >>> (bits - 64)));
  }

  /**
   * {@code result ^= operand}. Inputs must be the same length.
   * @param resultB the first input and the result
   * @param operandB the second input
   */
  public static void xorInto(ByteBuffer resultB, ByteBuffer operandB) {
    resultB.putLong(0, resultB.getLong(0) ^ operandB.getLong(0));
    resultB.putLong(Long.BYTES, resultB.getLong(Long.BYTES) ^ operandB.getLong(Long.BYTES));
  }

  /**
   * {@code result |= operand}. Inputs must be the same length.
   * @param resultB the first input and the result
   * @param operandB the second input
   */
  public static void orInto(ByteBuffer resultB, ByteBuffer operandB) {
    resultB.putLong(0, resultB.getLong(0) | operandB.getLong(0));
    resultB.putLong(Long.BYTES, resultB.getLong(Long.BYTES) | operandB.getLong(Long.BYTES));
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
