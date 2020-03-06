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

import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertBytesToLong;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Unit test for binary/hex utility methods.
 *
 * @author Daniel Dyer
 */
public class BinaryUtilsTest {

  private static final byte[] TEST_HEX_BYTES = {124, 11, 0, -76, -3, 127, -128, -1};
  private static final String TEST_HEX_STRING = "7C0B00B4FD7F80FF";
  private static final byte[] LONG_BYTES = {0, 0, 0, 16, 8, 4, 2, 1};
  private static final long LONG = 68853957121L;
  private static final byte[] INT_BYTES = {8, 4, 2, 1};
  private static final int INT = 134480385;

  private static void assertEqualsHex(final long actual, final long expected) {
    assertEquals(actual, expected, String.format("Expected %x, got %x", expected, actual));
  }

  @Test public void testConvertLongToBytes() {
    assertEquals(LONG_BYTES, BinaryUtils.convertLongToBytes(LONG), "Conversion gives wrong output");
  }

  @Test public void testConvertIntToBytes() {
    assertEquals(INT_BYTES, BinaryUtils.convertIntToBytes(INT), "Conversion gives wrong output");
  }

  @Test(timeOut = 1000) public void testConvertBytesToHexString() {
    final String generatedHex = BinaryUtils.convertBytesToHexString(TEST_HEX_BYTES);
    assertEquals(generatedHex, TEST_HEX_STRING, "Conversion gives wrong output");
  }

  @Test(timeOut = 1000) public void testConvertHexStringToBytes() {
    final byte[] generatedData = BinaryUtils.convertHexStringToBytes(TEST_HEX_STRING);
    assertEquals(generatedData, TEST_HEX_BYTES, "Conversion gives wrong output");
  }

  @Test(timeOut = 1000, expectedExceptions = IllegalArgumentException.class)
  public void testInvalidHexStringLength() {
    // Hex string should have even number of characters (2 per byte), so
    // this should throw an exception.
    BinaryUtils.convertHexStringToBytes("F2C");
  }

  /**
   * Make sure that the conversion method correctly converts 4 bytes to an integer assuming
   * big-endian convention.
   */
  @Test(timeOut = 1000) public void testConvertBytesToInt() {
    final int result = BinaryUtils.convertBytesToInt(INT_BYTES, 0);
    assertEquals(result, INT, "Conversion gives wrong output");
  }

  /**
   * Make sure that the conversion method correctly converts multiples of 4 bytes to an array of
   * integers assuming big-endian convention.
   */
  @Test(timeOut = 1000) public void testConvertBytesToInts() {
    final int expected1 = 16;
    final int[] result = BinaryUtils.convertBytesToInts(LONG_BYTES);
    assertEquals(result[0], expected1, "First integer is wrong");
    final int expected2 = 134480385;
    assertEquals(result[1], expected2, "Second integer is wrong");
  }

  /**
   * Make sure that the conversion method throws an exception if the number of bytes is not a
   * multiple of 4.
   */
  @Test(timeOut = 1000, expectedExceptions = IllegalArgumentException.class)
  public void testConvertWrongNumberOfBytesToInts() {
    final byte[] bytes = {0, 0, 16, 8, 4, 2, 1};
    BinaryUtils.convertBytesToInts(bytes);
  }

  /**
   * Make sure that the conversion method correctly converts 8 bytes to a long assuming big-endian
   * convention.
   */
  @Test(timeOut = 1000) public void testConvertBytesToLong() {
    final long result = convertBytesToLong(LONG_BYTES.clone());
    assertEqualsHex(result, LONG);
  }

  /**
   * Regression test for failure to correctly convert values that contain negative bytes.
   */
  @Test(timeOut = 1000) public void testConvertNegativeBytesToLong() {
    final byte[] bytes = new byte[]{-121, 30, 107, -100, -76, -8, 53, 81}.clone();
    final long expected = 0x871e6b9cb4f83551L;
    final long result = convertBytesToLong(bytes);
    assertEqualsHex(result, expected);
  }
}
