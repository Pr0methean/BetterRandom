package io.github.pr0methean.betterrandom.util;

/**
 * Collection of arithmetic methods that treat byte arrays as large fixed-precision integers. Each
 * byte is treated as unsigned, and the place value is 256 times the index in the array (thus it's
 * in little-endian form).
 */
public enum ByteArrayArithmetic {
  ;

  /**
   * {@code counter += delta}
   * @param delta the long-sized input
   * @param counter the variable-sized input and the result
   */
  public static void addLongToByteArrayInteger(long delta, byte[] counter) {
    byte[] addendDigits = new byte[counter.length];
    System.arraycopy(BinaryUtils.convertLongToBytes(delta), 0, addendDigits,
        counter.length - Long.BYTES, Long.BYTES);
    if (delta < 0) {
      // Sign extend
      for (int i = 0; i < counter.length - Long.BYTES; i++) {
        addendDigits[i] = -1;
      }
    }
    boolean carry = false;
    for (int i = 0; i < counter.length; i++) {
      byte oldCounter = counter[i];
      counter[i] += addendDigits[counter.length - i - 1] + (carry ? 1 : 0);
      carry = (counter[i] < oldCounter || (carry && counter[i] == oldCounter));
    }
  }
}
