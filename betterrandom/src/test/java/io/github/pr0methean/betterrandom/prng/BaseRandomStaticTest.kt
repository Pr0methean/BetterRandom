package io.github.pr0methean.betterrandom.prng

import io.github.pr0methean.betterrandom.prng.BaseRandom.entropyOfInt
import io.github.pr0methean.betterrandom.prng.BaseRandom.entropyOfLong
import org.testng.Assert.assertEquals

import io.github.pr0methean.betterrandom.TestingDeficiency
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass
import java.io.Serializable
import org.testng.annotations.Test

/**
 * Tests for [BaseRandom] that are not heritable by tests of subclasses.
 */
@Test(testName = "BaseRandom statics")
class BaseRandomStaticTest {

    @TestingDeficiency // FIXME: The switcheroo isn't happening!
    @Test(enabled = false)
    @Throws(IOException::class, ClassNotFoundException::class)
    fun testReadObjectNoData() {
        var switchedRandom: BaseRandom
        ByteArrayOutputStream().use { byteOutStream ->
            ObjectOutputStream(byteOutStream).use { objectOutStream ->
                objectOutStream.writeObject(Switcheroo())
                val serialCopy = byteOutStream.toByteArray()
                // Read the object back-in.
                SwitcherooInputStream(
                        ByteArrayInputStream(serialCopy)).use { objectInStream ->
                    switchedRandom = objectInStream.readObject() as BaseRandom // ClassCastException
                }
            }
        }
        switchedRandom.nextInt()
    }

    @Test
    fun testEntropyOfInt() {
        assertEquals(entropyOfInt(0, 1), 0)
        assertEquals(entropyOfInt(0, 2), 1)
        assertEquals(entropyOfInt(0, 1 shl 24), 24)
        assertEquals(entropyOfInt(1 shl 22, 1 shl 24), 24)
        assertEquals(entropyOfInt(-(1 shl 24), 0), 24)
        assertEquals(entropyOfInt(-(1 shl 24), 1), 25)
    }

    @Test
    fun testEntropyOfLong() {
        assertEquals(entropyOfLong(0, 1), 0)
        assertEquals(entropyOfLong(0, 2), 1)
        assertEquals(entropyOfLong(0, 1L shl 32), 32)
        assertEquals(entropyOfLong(0, 1L shl 42), 42)
        assertEquals(entropyOfLong(0, java.lang.Long.MAX_VALUE), 63)
        assertEquals(entropyOfLong(java.lang.Long.MIN_VALUE, java.lang.Long.MAX_VALUE), 64)
        assertEquals(entropyOfLong((1 shl 22).toLong(), 1L shl 42), 42)
        assertEquals(entropyOfLong(-(1L shl 42), 0), 42)
        assertEquals(entropyOfLong(-(1L shl 42), 1), 43)
    }

    private class Switcheroo : Serializable {
        companion object {

            private const val serialVersionUID = 5949778642428995210L
        }
    }

    private class SwitcherooInputStream @Throws(IOException::class)
    constructor(`in`: InputStream) : ObjectInputStream(`in`) {

        @Throws(IOException::class, ClassNotFoundException::class)
        override fun resolveClass(desc: ObjectStreamClass): Class<*> {
            return if (Switcheroo.serialVersionUID == desc.serialVersionUID)
                AesCounterRandom::class.java
            else
                super.resolveClass(desc)
        }
    }
}
