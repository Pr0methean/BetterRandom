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
package io.github.pr0methean.betterrandom.util

import io.github.pr0methean.betterrandom.util.BinaryUtils.convertBytesToLong

import java.util.Arrays
import org.testng.annotations.Test

/**
 * Unit test for binary/hex utility methods.
 * @author Daniel Dyer
 */
class BinaryUtilsTest {

    @Test
    @Throws(Exception::class)
    fun testConvertLongToBytes() {
        assert(Arrays.equals(LONG_BYTES, BinaryUtils.convertLongToBytes(LONG)))
    }

    @Test
    @Throws(Exception::class)
    fun testConvertIntToBytes() {
        assert(Arrays.equals(INT_BYTES, BinaryUtils.convertIntToBytes(INT)))
    }

    @Test(timeOut = 1000)
    fun testConvertBytesToHexString() {
        val generatedHex = BinaryUtils.convertBytesToHexString(TEST_HEX_BYTES)
        assert(generatedHex == TEST_HEX_STRING) { "Wrong hex string: $generatedHex" }
    }

    @Test(timeOut = 1000)
    fun testConvertHexStringToBytes() {
        val generatedData = BinaryUtils.convertHexStringToBytes(TEST_HEX_STRING)
        assert(Arrays.equals(generatedData, TEST_HEX_BYTES)) {
            "Wrong byte array: " + Arrays
                    .toString(generatedData)
        }
    }

    @Test(timeOut = 1000, expectedExceptions = arrayOf(IllegalArgumentException::class))
    fun testInvalidHexStringLength() {
        // Hex string should have even number of characters (2 per byte), so
        // this should throw an exception.
        BinaryUtils.convertHexStringToBytes("F2C")
    }

    /**
     * Make sure that the conversion method correctly converts 4 bytes to an integer assuming
     * big-endian convention.
     */
    @Test(timeOut = 1000)
    fun testConvertBytesToInt() {
        val result = BinaryUtils.convertBytesToInt(INT_BYTES, 0)
        assert(INT == result) { "Expected $INT, was $result" }
    }

    /**
     * Make sure that the conversion method correctly converts multiples of 4 bytes to an array of
     * integers assuming big-endian convention.
     */
    @Test(timeOut = 1000)
    fun testConvertBytesToInts() {
        val expected1 = 16
        val result = BinaryUtils.convertBytesToInts(LONG_BYTES)
        assert(expected1 == result[0]) { "Expected first int to be " + expected1 + ", was " + result[0] }
        val expected2 = 134480385
        assert(expected2 == result[1]) { "Expected second int to be " + expected2 + ", was " + result[1] }
    }

    /**
     * Make sure that the conversion method throws an exception if the number of bytes is not a
     * multiple of 4.
     */
    @Test(timeOut = 1000, expectedExceptions = arrayOf(IllegalArgumentException::class))
    fun testConvertWrongNumberOfBytesToInts() {
        val bytes = byteArrayOf(0, 0, 16, 8, 4, 2, 1)
        BinaryUtils.convertBytesToInts(bytes)
    }

    /**
     * Make sure that the conversion method correctly converts 8 bytes to a long assuming big-endian
     * convention.
     */
    @Test(timeOut = 1000)
    fun testConvertBytesToLong() {
        val result = convertBytesToLong(LONG_BYTES)
        assert(LONG == result) { "Expected $LONG, was $result" }
    }

    /**
     * Regression test for failure to correctly convert values that contain negative bytes.
     */
    @Test(timeOut = 1000)
    fun testConvertNegativeBytesToLong() {
        val bytes = byteArrayOf(-121, 30, 107, -100, -76, -8, 53, 81)
        val expected = -510639L
        val result = convertBytesToLong(bytes)
        assert(expected == result) { "Expected $expected, was $result" }
    }

    companion object {

        private val TEST_HEX_BYTES = byteArrayOf(124, 11, 0, -76, -3, 127, -128, -1)
        private val TEST_HEX_STRING = "7C0B00B4FD7F80FF"
        private val LONG_BYTES = byteArrayOf(0, 0, 0, 16, 8, 4, 2, 1)
        private val LONG = 68853957121L
        private val INT_BYTES = byteArrayOf(8, 4, 2, 1)
        private val INT = 134480385
    }
}
