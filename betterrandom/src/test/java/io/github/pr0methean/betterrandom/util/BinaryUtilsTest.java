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

import java.nio.ByteOrder;
import java.util.Arrays;
import org.testng.annotations.Test;

/**
 * Unit test for binary/hex utility methods.
 * @author Daniel Dyer
 */
public class BinaryUtilsTest {

  private static final byte[] TEST_HEX_BYTES = {124, 11, 0, -76, -3, 127, -128, -1};
  private static final String TEST_HEX_STRING = "7C0B00B4FD7F80FF";
  private static final byte[] LONG_BYTES = {0, 0, 0, 16, 8, 4, 2, 1};
  private static final long LONG = 68853957121L;
  private static final byte[] INT_BYTES = {8, 4, 2, 1};
  private static final int INT = 134480385;

  private byte[] fromBigEndian(byte[] in) {
    byte[] out = in.clone();
    if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
      // Reverse the copy
      for (int i = 0; i < out.length / 2; i++) {
        byte temp = out[i];
        out[i] = out[out.length - 1 - i];
        out[out.length - 1 - i] = temp;
      }
    }
    return out;
  }

  private void assertEqualsHex(long actual, long expected) {
    assertEquals(actual, expected, String.format("Expected %x, got %x", actual, expected));
  }

  @Test public void testConvertLongToBytes() throws Exception {
    assert Arrays.equals(fromBigEndian(LONG_BYTES), BinaryUtils.convertLongToBytes(LONG));
  }

  @Test public void testConvertIntToBytes() throws Exception {
    assert Arrays.equals(fromBigEndian(INT_BYTES), BinaryUtils.convertIntToBytes(INT));
  }

  @Test(timeOut = 1000) public void testConvertBytesToHexString() {
    final String generatedHex = BinaryUtils.convertBytesToHexString(TEST_HEX_BYTES);
    assert generatedHex.equals(TEST_HEX_STRING) : "Wrong hex string: " + generatedHex;
  }

  @Test(timeOut = 1000) public void testConvertHexStringToBytes() {
    final byte[] generatedData = BinaryUtils.convertHexStringToBytes(TEST_HEX_STRING);
    assert Arrays.equals(generatedData, TEST_HEX_BYTES) : "Wrong byte array: " + Arrays
        .toString(generatedData);
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
    assert INT == result : "Expected " + INT + ", was " + result;
  }

  /**
   * Make sure that the conversion method correctly converts multiples of 4 bytes to an array of
   * integers assuming big-endian convention.
   */
  @Test(timeOut = 1000) public void testConvertBytesToInts() {
    final int expected1 = 16;
    final int[] result = BinaryUtils.convertBytesToInts(LONG_BYTES);
    assert expected1 == result[0] : "Expected first int to be " + expected1 + ", was " + result[0];
    final int expected2 = 134480385;
    assert expected2 == result[1] : "Expected second int to be " + expected2 + ", was " + result[1];
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
    final long result = convertBytesToLong(fromBigEndian(LONG_BYTES));
    assertEqualsHex(result, LONG);
  }

  /**
   * Regression test for failure to correctly convert values that contain negative bytes.
   */
  @Test(timeOut = 1000) public void testConvertNegativeBytesToLong() {
    final byte[] bytes = fromBigEndian(new byte[]{-121, 30, 107, -100, -76, -8, 53, 81});
    final long expected = 0xfffffffffff83551L;
    final long result = convertBytesToLong(bytes);
    assertEqualsHex(result, expected);
  }
}
