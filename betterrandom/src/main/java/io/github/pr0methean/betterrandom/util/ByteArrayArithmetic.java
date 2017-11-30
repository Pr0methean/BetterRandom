package io.github.pr0methean.betterrandom.util;

/**
 * Collection of arithmetic methods that treat byte arrays as large fixed-precision integers. Each
 * byte is treated as unsigned, and the array is in {@link java.nio.ByteOrder#BIG_ENDIAN}.
 */
public enum ByteArrayArithmetic {
  ;

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
    for (int i = counter.length - 1; i > 0; i--) {
      final byte oldCounter = counter[i];
      counter[i] += delta[i] + (carry ? 1 : 0);
      carry = ((counter[i] < oldCounter) || (carry && (counter[i] == oldCounter)));
    }
  }

  /**
   * {@code counter *= multiplier}
   * @param counter the first input and the result
   * @param multiplier the second input
   */
  public static void multiplyInto(byte[] counter, byte[] multiplier) {
    // TODO
  }

  /**
   * {@code shifted >>>= bits}
   * @param shifted the array input and the result
   * @param bits how many bits to shift by
   */
  public static void unsignedShiftRight(byte[] shifted, int bits) {
    // TODO
  }

  /**
   * {@code result ^= operand}. Inputs must be the same length.
   * @param result the first input and the result
   * @param operand
   */
  public static void xorInto(byte[] result, byte[] operand) {
    for (int i=0; i < result.length; i++) {
      result[i] ^= operand[i];
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
}
