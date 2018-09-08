package io.github.pr0methean.betterrandom.util;

import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertBytesToInt;
import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertBytesToLong;
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
  private static final ThreadLocal<byte[]> addendDigits = makeByteArrayThreadLocal();
  private static final ThreadLocal<byte[]> multAccumulator = makeByteArrayThreadLocal();
  private static final long UNSIGNED_INT_TO_LONG_MASK = (1L << Integer.SIZE) - 1;

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
    addInto(counter, addendDigits);  }

  /**
   * {@code counter += delta << (8 * offsetBytes)}
   * @param counter the variable-sized input and the result
   * @param delta the long-sized input
   * @param signed if true, treat {@code delta} as signed
   * @param offsetBytes the number of bytes to shift by
   */
  public static void addInto(byte[] counter, long delta, boolean signed, int offsetBytes) {
    byte[] addendDigits = Byte16ArrayArithmetic.addendDigits.get();
    BinaryUtils.convertLongToBytesTruncating(delta, addendDigits, SIZE_BYTES_MINUS_LONG - offsetBytes);
    final byte signExtend = (byte) ((signed && (delta < 0)) ? -1 : 0);
    for (int i = 0; i < SIZE_BYTES_MINUS_LONG - offsetBytes; i++) {
      addendDigits[i] = signExtend;
    }
    for (int i = SIZE_BYTES - offsetBytes; i < SIZE_BYTES; i++) {
      addendDigits[i] = 0;
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

  private static long doMultiplicationLimb(byte[] op1, byte[] op2, int limb1, int limb2)
  {
    return (convertBytesToInt(op1, Integer.BYTES * limb1) & UNSIGNED_INT_TO_LONG_MASK)
        * (convertBytesToInt(op2, Integer.BYTES * limb2) & UNSIGNED_INT_TO_LONG_MASK);
  }

  /**
   * {@code counter *= multiplier}
   * @param counter the first input and the result
   * @param mult the second input
   */
  @SuppressWarnings("NumericCastThatLosesPrecision") public static void multiplyInto(
      byte[] counter, byte[] mult) {
    long x = convertBytesToLong(counter, Long.BYTES);
    long y = convertBytesToLong(mult, Long.BYTES);

    // https://stackoverflow.com/a/38880097/833771
    long x_high = x >>> 32;
    long x_low = x & 0xFFFFFFFFL;
    long y_high = y >>> 32;
    long y_low = y & 0xFFFFFFFFL;
    long z2 = x_low * y_low;
    long t = x_high * y_low + (z2 >>> 32);
    long z1 = t & 0xFFFFFFFFL;
    long z0 = t >>> 32;
    z1 += x_low * y_high;
    long highOut = x_high * y_high + z0 + (z1 >>> 32) + convertBytesToLong(counter, 0) * y
        + convertBytesToLong(mult, 0) * x;

    long lowOut = x * y;
    convertLongToBytes(highOut, counter, 0);
    convertLongToBytes(lowOut, counter, Long.BYTES);
  }

  private static long trueShiftRight(long input, int amount) {
    if (amount <= -Long.SIZE || amount >= Long.SIZE) {
      return 0;
    }
    if (amount < 0) {
      return input << -amount;
    }
    return input >>> amount;
  }

  /**
   * {@code shifted >>>= bits}
   * From <a href="https://github.com/patrickfav/bytes-java/blob/743a6ab60649e6ce7ec972412bdcb42010a46077/src/main/java/at/favre/lib/bytes/Util.java#L395">this source</a>.
   * @param shifted the array input and the result
   * @param bits how many bits to shift by
   * @author Patrick Favre-Bulle
   */
  public static void unsignedShiftRight(byte[] shifted, int bits) {
    if (bits == 0) {
      return;
    }
    long oldMost = convertBytesToLong(shifted);
    long oldLeast = convertBytesToLong(shifted, Long.BYTES);
    convertLongToBytes(shiftedMost(bits, oldMost, oldLeast), shifted, 0);
    convertLongToBytes(shiftedLeast(bits, oldMost, oldLeast), shifted, Long.BYTES);
  }

  private static long shiftedMost(int bits, long oldMost, long oldLeast) {
    return trueShiftRight(oldMost, bits) | trueShiftRight(oldLeast, bits + 64);
  }

  private static long shiftedLeast(int bits, long oldMost, long oldLeast) {
    return trueShiftRight(oldLeast, bits) | trueShiftRight(oldMost, bits - 64);
  }

  /**
   * {@code shifted = (shifted >>> bits) | shifted << (128 - bits)}
   * @param shifted the array input and the result
   * @param bits how many bits to shift by
   * @author Patrick Favre-Bulle
   */
  public static void rotateRight(byte[] shifted, int bits) {
    bits %= 128;
    if (bits == 0) {
      return;
    }
    if (bits < 0) {
      bits += 128;
    }
    long oldMost = convertBytesToLong(shifted);
    long oldLeast = convertBytesToLong(shifted, Long.BYTES);
    convertLongToBytes(
        shiftedMost(bits, oldMost, oldLeast) | shiftedMost(otherShift(bits), oldMost, oldLeast),
        shifted, 0);
    convertLongToBytes(rotateRightLeast64(bits, oldMost, oldLeast),
        shifted, Long.BYTES);
  }

  private static long rotateRightLeast64(int bits, long oldMost, long oldLeast) {
    return shiftedLeast(bits, oldMost, oldLeast) | shiftedLeast(otherShift(bits), oldMost, oldLeast);
  }

  private static int otherShift(int bits) {
    return bits > 0 ? bits - 128 : bits + 128;
  }

  /**
   * {@code return (long) ((shifted >>> bits) | shifted << (128 - bits))}
   * @param shifted the array input and the result
   * @param bits how many bits to shift by
   */
  public static long rotateRightLeast64(byte[] shifted, int bits) {
    return rotateRightLeast64(bits, convertBytesToLong(shifted),
        convertBytesToLong(shifted, Long.BYTES));
  }

  /**
   * {@code result ^= operand}. Inputs must be the same length.
   * @param result the first input and the result
   * @param operandB the second input
   */
  public static void xorInto(byte[] result, byte[] operandB) {
    for (int i = 0; i < SIZE_BYTES; i++) {
      result[i] ^= operandB[i];
    }
  }

  /**
   * {@code result |= operand}. Inputs must be the same length.
   * @param result the first input and the result
   * @param operandB the second input
   */
  public static void orInto(byte[] result, byte[] operandB) {
    for (int i = 0; i < SIZE_BYTES; i++) {
      result[i] |= operandB[i];
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
