package io.github.pr0methean.betterrandom

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

enum class CloneViaSerialization {
    ;

    companion object {

        /**
         * Clones an object by serializing and deserializing it.
         * @param object The object to clone.
         * @param <T> The type of `object`.
         * @return A clone of `object`.
        </T> */
        fun <T : Serializable> clone(`object`: T): T {
            return fromByteArray(toByteArray(`object`))
        }

        fun <T : Serializable> fromByteArray(serialCopy: ByteArray): T {
            try {
                ObjectInputStream(
                        ByteArrayInputStream(serialCopy)).use { objectInStream -> return objectInStream.readObject() as T }
            } catch (e: IOException) {
                throw RuntimeException(e)
            } catch (e: ClassNotFoundException) {
                throw RuntimeException(e)
            }

        }

        fun <T : Serializable> toByteArray(`object`: T): ByteArray {
            try {
                ByteArrayOutputStream().use { byteOutStream ->
                    ObjectOutputStream(byteOutStream).use { objectOutStream ->
                        objectOutStream.writeObject(`object`)
                        return byteOutStream.toByteArray()
                    }
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }

        }
    }
}/* Utility class with no instances */
