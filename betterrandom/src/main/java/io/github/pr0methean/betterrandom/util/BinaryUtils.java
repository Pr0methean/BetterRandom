// ============================================================================
//   Copyright 2006-2012 Daniel W. Dyer
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
// ============================================================================
package io.github.pr0methean.betterrandom.util;

/**
 * Utility methods for working with binary and hex data.
 * @author Daniel Dyer
 */
public enum BinaryUtils {
  ;

  // Mask for casting a byte to an int, bit-by-bit (with
  // bitwise AND) with no special consideration for the sign bit.
  private static final int BITWISE_BYTE_TO_INT = 0x000000FF;
  private static final long BITWISE_BYTE_TO_LONG = BITWISE_BYTE_TO_INT;

  /**
   * Take eight bytes from the specified position in the specified block and convert them into a
   * long, using the big-endian convention.
   * @param bytes The data to read from.
   * @param offset The position to start reading the long from.
   * @return The 32-bit integer represented by the eight bytes.
   */
  public static long convertBytesToLong(final byte[] bytes, final int offset) {
    return (BITWISE_BYTE_TO_LONG & bytes[offset + 7])
        | ((BITWISE_BYTE_TO_LONG & bytes[offset + 6]) << 8L)
        | ((BITWISE_BYTE_TO_LONG & bytes[offset + 5]) << 16L)
        | ((BITWISE_BYTE_TO_LONG & bytes[offset + 4]) << 24L)
        | ((BITWISE_BYTE_TO_LONG & bytes[offset + 3]) << 32L)
        | ((BITWISE_BYTE_TO_LONG & bytes[offset + 2]) << 40L)
        | ((BITWISE_BYTE_TO_LONG & bytes[offset + 1]) << 48L)
        | ((BITWISE_BYTE_TO_LONG & bytes[offset]) << 56L);
  }

  /**
   * Convert a byte array to a long.
   * @param bytes a byte array of length {@link Long#BYTES} in
   *     {@link java.nio.ByteOrder#nativeOrder()} order.
   * @return {@code bytes} as a long.
   */
  public static long convertBytesToLong(final byte[] bytes) {
    return convertBytesToLong(bytes, 0);
  }
}
