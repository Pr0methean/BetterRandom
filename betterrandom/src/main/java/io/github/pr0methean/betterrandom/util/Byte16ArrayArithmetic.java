package io.github.pr0methean.betterrandom.util;

import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertBytesToLong;
import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertLongToBytes;
import static io.github.pr0methean.betterrandom.util.Java8Constants.LONG_BYTES;

/**
 * Collection of arithmetic methods that treat {@code byte[16]} arrays as 128-bit unsigned integers.
 */
public enum Byte16ArrayArithmetic {
  ;

  private static final int SIZE_BYTES = 16;
  private static final int SIZE_BYTES_MINUS_LONG = SIZE_BYTES - LONG_BYTES;

  /**
   * The 128-bit value 0.
   */
  public static final byte[] ZERO = new byte[SIZE_BYTES];

  /**
   * The 128-bit value 1.
   */
  public static final byte[] ONE = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
  private static final long UNSIGNED_INT_TO_LONG_MASK = (1L << Integer.SIZE) - 1;

  /**
   * {@code counter += delta}
   *  @param counter the variable-sized input and the result
   * @param delta the long-sized input
   */
  public static void addInto(final byte[] counter, final long delta) {
    byte[] addendDigits = new byte[16];
    convertLongToBytes(delta, addendDigits, SIZE_BYTES_MINUS_LONG);
    final byte signExtend = (byte) (delta < 0 ? -1 : 0);
    for (int i = 0; i < SIZE_BYTES_MINUS_LONG; i++) {
      addendDigits[i] = signExtend;
    }
    addInto(counter, addendDigits);
  }

  /**
   * {@code counter += delta}. Inputs must be the same length.
   *
   * @param counter the first input and the result
   * @param delta the second input
   */
  public static void addInto(final byte[] counter, final byte[] delta) {
    boolean carry = false;
    for (int i = 15; i >= 0; i--) {
      final int oldCounterUnsigned = counter[i] < 0 ? counter[i] + 256 : counter[i];
      counter[i] += delta[i] + (carry ? 1 : 0);
      final int newCounterUnsigned = counter[i] < 0 ? counter[i] + 256 : counter[i];
      carry = (oldCounterUnsigned > newCounterUnsigned) ||
          (carry && (oldCounterUnsigned == newCounterUnsigned));
    }
  }

  /**
   * {@code counter *= multiplier}
   *
   * @param counter the first input and the result
   * @param mult the second input
   */
  public static void multiplyInto(
      final byte[] counter, final byte[] mult) {
    multiplyIntoAndAddInto(counter, mult, 0, 0);
  }

  /**
   * {@code counter *= mult; counter += add}
   *
   * @param counter the first input and the result
   * @param mult the input to multiply by
   * @param add the input to add after multiplying
   */
  public static void multiplyIntoAndAddInto(final byte[] counter, final byte[] mult,
      final byte[] add) {
    multiplyIntoAndAddInto(counter, mult, convertBytesToLong(add, LONG_BYTES),
        convertBytesToLong(add, 0));
  }

  /**
   * {@code counter *= mult; counter += addHigh <<< 64 + addLow;}
   *
   * @param counter the first input and the result
   * @param mult the input to multiply by
   * @param addLow low 64 bits to add
   * @param addHigh high 64 bits to add
   */
  private static void multiplyIntoAndAddInto(
      final byte[] counter, final byte[] mult, long addLow, long addHigh) {
    final long x = convertBytesToLong(counter, LONG_BYTES);
    final long y = convertBytesToLong(mult, LONG_BYTES);

    // https://stackoverflow.com/a/38880097/833771
    final long x_high = x >>> 32;
    final long x_low = x & UNSIGNED_INT_TO_LONG_MASK;
    final long y_high = y >>> 32;
    final long y_low = y & UNSIGNED_INT_TO_LONG_MASK;
    final long t = x_high * y_low + (x_low * y_low >>> 32);
    final long z1 = (t & UNSIGNED_INT_TO_LONG_MASK) + x_low * y_high;
    final long z0 = t >>> 32;
    final long lowProduct = x * y;
    final long lowOut = lowProduct + addLow;
    final long highOut =
        (x_high * y_high) + z0 + (z1 >>> 32) + (convertBytesToLong(counter, 0) * y) +
            (convertBytesToLong(mult, 0) * x) + addHigh +
            ((lowProduct + Long.MIN_VALUE > lowOut + Long.MIN_VALUE) ? 1 : 0);

    convertLongToBytes(highOut, counter, 0);
    convertLongToBytes(lowOut, counter, LONG_BYTES);
  }

  private static long trueShiftRight(final long input, final int amount) {
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
   * From
   * <a href="https://github.com/patrickfav/bytes-java/blob/743a6ab60649e6ce7ec972412bdcb42010a46077/src/main/java/at/favre/lib/bytes/Util.java#L395">this source</a>.
   *
   * @param shifted the array input and the result
   * @param bits how many bits to shift by
   */
  public static void unsignedShiftRight(final byte[] shifted, final int bits) {
    if (bits == 0) {
      return;
    }
    final long oldMost = convertBytesToLong(shifted);
    final long oldLeast = convertBytesToLong(shifted, LONG_BYTES);
    convertLongToBytes(shiftedMost(bits, oldMost, oldLeast), shifted, 0);
    convertLongToBytes(shiftedLeast(bits, oldMost, oldLeast), shifted, LONG_BYTES);
  }

  /**
   * Returns the lower 64 bits of the shifted input.
   * From
   * <a href="https://github.com/patrickfav/bytes-java/blob/743a6ab60649e6ce7ec972412bdcb42010a46077/src/main/java/at/favre/lib/bytes/Util.java#L395">this source</a>.
   *
   * @param shifted the array input and the result
   * @param bits how many bits to shift by
   * @return {@code (long)(shifted >>> bits)}
   */
  public static long unsignedShiftRightLeast64(final byte[] shifted, final int bits) {
    final long oldLeast = convertBytesToLong(shifted, LONG_BYTES);
    if (bits == 0) {
      return oldLeast;
    }
    final long oldMost = convertBytesToLong(shifted);
    return shiftedLeast(bits, oldMost, oldLeast);
  }

  /**
   * Returns the upper 64 bits of {@code (oldMost << 64LL + oldLeast) >>> bits}.
   *
   * @param bits how many bits to shift by
   * @param oldMost upper 64 bits of input
   * @param oldLeast lower 64 bits of input
   * @return the upper 64 bits of {@code (oldMost << 64LL + oldLeast) >>> bits}
   */
  public static long shiftedMost(final int bits, final long oldMost, final long oldLeast) {
    return trueShiftRight(oldMost, bits) | trueShiftRight(oldLeast, bits + 64);
  }

  /**
   * Returns the lower 64 bits of {@code (oldMost << 64LL + oldLeast) >>> bits}.
   *
   * @param bits how many bits to shift by
   * @param oldMost upper 64 bits of input
   * @param oldLeast lower 64 bits of input
   * @return the lower 64 bits of {@code (oldMost << 64LL + oldLeast) >>> bits}
   */
  public static long shiftedLeast(final int bits, final long oldMost, final long oldLeast) {
    return trueShiftRight(oldLeast, bits) | trueShiftRight(oldMost, bits - 64);
  }

  /**
   * {@code shifted = (shifted >>> bits) | shifted << (128 - bits)}
   *
   * @param shifted the array input and the result
   * @param bits how many bits to shift by
   */
  public static void rotateRight(final byte[] shifted, int bits) {
    bits %= 128;
    if (bits == 0) {
      return;
    }
    if (bits < 0) {
      bits += 128;
    }
    final long oldMost = convertBytesToLong(shifted);
    final long oldLeast = convertBytesToLong(shifted, LONG_BYTES);
    convertLongToBytes(
        shiftedMost(bits, oldMost, oldLeast) | shiftedMost(otherShift(bits), oldMost, oldLeast),
        shifted, 0);
    convertLongToBytes(rotateRightLeast64(bits, oldMost, oldLeast),
        shifted, LONG_BYTES);
  }

  private static long rotateRightLeast64(final int bits, final long oldMost, final long oldLeast) {
    return shiftedLeast(bits, oldMost, oldLeast) |
        shiftedLeast(otherShift(bits), oldMost, oldLeast);
  }

  private static int otherShift(final int bits) {
    return bits > 0 ? bits - 128 : bits + 128;
  }

  /**
   * Returns the lower 64 bits of the result when the input is rotated.
   *
   * @param shifted the array input and the result
   * @param bits how many bits to shift by
   * @return {@code (long) ((shifted >>> bits) | shifted << (128 - bits))}
   */
  public static long rotateRightLeast64(final byte[] shifted, final int bits) {
    return rotateRightLeast64(bits, convertBytesToLong(shifted),
        convertBytesToLong(shifted, LONG_BYTES));
  }

}
