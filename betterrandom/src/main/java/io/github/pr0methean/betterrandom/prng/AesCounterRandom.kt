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
package io.github.pr0methean.betterrandom.prng

import com.google.common.base.MoreObjects.ToStringHelper
import io.github.pr0methean.betterrandom.SeekableRandom
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator
import io.github.pr0methean.betterrandom.seed.SeedException
import io.github.pr0methean.betterrandom.seed.SeedGenerator
import io.github.pr0methean.betterrandom.util.BinaryUtils
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Arrays
import java.util.Random
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.SecretKeySpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 *
 * Non-linear random number generator based on the AES block cipher in counter mode. Uses the
 * seed as a key to encrypt a 128-bit counter using AES(Rijndael).
 *
 *By default, we only use a
 * 128-bit key for the cipher because any larger key requires the inconvenience of installing the
 * unlimited strength cryptography policy files for the Java platform.  Larger keys may be used (192
 * or 256 bits) but if the cryptography policy files are not installed, a [ ] will be thrown.
 *
 **NOTE: Because instances of this class
 * require 128-bit seeds, it is not possible to seed this RNG using the [.setSeed]
 * method inherited from [Random] until the seed array has been set.*
 * @author Daniel Dyer
 * @author Chris Hennick
 */
class AesCounterRandom
/**
 * Creates an RNG and seeds it with the specified seed data.
 * @param seed The seed data used to initialise the RNG. Length must be at least 16 and no
 * more than [.getMaxKeyLengthBytes] + 16.
 */
(seed: ByteArray) : BaseRandom(seed), SeekableRandom {

    private val currentBlock: ByteArray?
    // WARNING: Don't initialize any instance fields at declaration; they may be initialized too late!
    @Transient
    private var cipher: Cipher? = null
    @Volatile
    private var counter: ByteArray? = null
    @Volatile
    private var counterInput: ByteArray? = null
    @Volatile
    private var seeded: Boolean = false
    @Volatile
    private var index: Int = 0

    /** Returns the longest supported seed length.  */
    override val newSeedLength: Int
        get() = MAX_TOTAL_SEED_LENGTH_BYTES

    /**
     * Seed the RNG using the provided seed generation strategy to create a 256-bit seed.
     * @param seedGenerator The seed generation strategy that will provide the seed value for this
     * RNG.
     * @throws SeedException if there is a problem generating a seed.
     */
    @Throws(SeedException::class)
    constructor(seedGenerator: SeedGenerator) : this(seedGenerator.generateSeed(DEFAULT_SEED_SIZE_BYTES)) {
    }

    /**
     * Seed the RNG using the [DefaultSeedGenerator] to create a seed of the specified size.
     * @param seedSizeBytes The number of bytes to use for seed data. Valid values range from 16
     * to [.getMaxKeyLengthBytes] + 16.
     * @throws SeedException if the [DefaultSeedGenerator] fails to generate a seed.
     */
    @Throws(SeedException::class)
    @JvmOverloads constructor(seedSizeBytes: Int = DEFAULT_SEED_SIZE_BYTES) : this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(seedSizeBytes)) {
    }

    init {
        currentBlock = ByteArray(BYTES_AT_ONCE)
        index = BYTES_AT_ONCE // force generation of first block on demand
    }

    public override fun addSubclassFields(original: ToStringHelper): ToStringHelper {
        return original.add("counter", BinaryUtils.convertBytesToHexString(counter))
                .add("cipher", cipher!!)
                .add("index", index)
    }

    override fun initTransientFields() {
        super.initTransientFields()
        if (counter == null) {
            counter = ByteArray(COUNTER_SIZE_BYTES)
        }
        if (counterInput == null) {
            counterInput = ByteArray(BYTES_AT_ONCE)
        }
        try {
            cipher = Cipher.getInstance(ALGORITHM_MODE)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("JVM doesn't provide $ALGORITHM_MODE", e)
        } catch (e: NoSuchPaddingException) {
            throw RuntimeException("JVM doesn't provide $ALGORITHM_MODE", e)
        }

    }

    private fun incrementCounter() {
        for (i in counter!!.indices) {
            ++counter!![i]
            if (counter!![i].toInt() != 0)
            // Check whether we need to loop again to carry the one.
            {
                break
            }
        }
    }

    /**
     * Generates BLOCKS_AT_ONCE 128-bit (16-byte) blocks. Copies them to currentBlock.
     * @throws IllegalStateException If there is a problem with the cipher that generates the
     * random data.
     */
    private fun nextBlock() {
        for (i in 0 until BLOCKS_AT_ONCE) {
            incrementCounter()
            System.arraycopy(counter!!, 0, counterInput!!, i * COUNTER_SIZE_BYTES, COUNTER_SIZE_BYTES)
        }
        try {
            cipher!!.doFinal(counterInput!!, 0, COUNTER_SIZE_BYTES * BLOCKS_AT_ONCE, currentBlock)
        } catch (ex: GeneralSecurityException) {
            // Should never happen.  If initialisation succeeds without exceptions
            // we should be able to proceed indefinitely without exceptions.
            throw IllegalStateException("Failed creating next random block.", ex)
        }

    }

    override fun next(bits: Int): Int {
        lock.lock()
        val result: Int
        try {
            if (BYTES_AT_ONCE - index < 4) {
                nextBlock()
                index = 0
            }
            result = BinaryUtils.convertBytesToInt(currentBlock!!, index)
            index += 4
        } finally {
            lock.unlock()
        }
        return result.ushr(32 - bits)
    }

    /**
     * {@inheritDoc} If the seed is not of the maximum length, it is combined with the existing seed
     * using SHA-256.
     */
    override fun setSeed(seed: ByteArray) {
        if (seed.size > MAX_TOTAL_SEED_LENGTH_BYTES) {
            throw IllegalArgumentException(
                    "Seed too long: maximum $MAX_TOTAL_SEED_LENGTH_BYTES bytes")
        }
        try {
            val key: ByteArray
            if (seed.size == maxKeyLengthBytes) {
                key = seed.clone()
            } else {
                lock.lock()
                val weAreSeeded: Boolean
                try {
                    weAreSeeded = seeded
                } finally {
                    lock.unlock()
                }
                if (weAreSeeded) {
                    // Extend the key
                    val newSeed = ByteArray(this.seed!!.size + seed.size)
                    System.arraycopy(this.seed!!, 0, newSeed, 0, this.seed!!.size)
                    System.arraycopy(seed, 0, newSeed, this.seed!!.size, seed.size)
                    val keyLength = getKeyLength(newSeed)
                    if (newSeed.size > keyLength) {
                        val md = MessageDigest.getInstance(HASH_ALGORITHM)
                        md.update(newSeed)
                        key = Arrays.copyOf(md.digest(), keyLength)
                    } else {
                        key = newSeed
                    }
                } else {
                    key = seed.clone()
                }
            }
            lock.lock()
            try {
                setSeedInternal(key)
                entropyBits.addAndGet(8L * (seed.size - key.size))
            } finally {
                lock.unlock()
            }
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }

    }

    /**
     * Combines the given seed with the existing seed using SHA-256.
     */
    override fun setSeed(seed: Long) {
        if (superConstructorFinished) {
            val seedBytes = BinaryUtils.convertLongToBytes(seed)
            setSeed(seedBytes)
        }
    }

    override fun setSeedInternal(seed: ByteArray?) {
        val seedLength = seed!!.size
        if (seedLength < 16 || seedLength > MAX_TOTAL_SEED_LENGTH_BYTES) {
            throw IllegalArgumentException(String
                    .format("Seed length is %d bytes; need 16 to %d bytes", seedLength,
                            MAX_TOTAL_SEED_LENGTH_BYTES))
        }
        super.setSeedInternal(seed)
        // determine how much of seed can go to key
        val keyLength = getKeyLength(seed)
        val key = Arrays.copyOfRange(seed, 0, keyLength)
        // rest goes to counter
        val bytesToCopyToCounter = seedLength - keyLength
        System.arraycopy(seed, keyLength, counter!!, 0, bytesToCopyToCounter)
        System.arraycopy(ZEROES, 0, counter!!, bytesToCopyToCounter,
                COUNTER_SIZE_BYTES - bytesToCopyToCounter)
        try {
            cipher!!.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, ALGORITHM))
        } catch (e: InvalidKeyException) {
            throw RuntimeException("Invalid key: " + Arrays.toString(key), e)
        }

        if (currentBlock != null) {
            index = BYTES_AT_ONCE
        } // else it'll be initialized in ctor
        seeded = true
    }

    override fun advance(delta: Long) {
        if (delta == 0L) {
            return
        }
        var blocksDelta = delta / INTS_PER_BLOCK
        val deltaWithinBlock = (delta % INTS_PER_BLOCK).toInt() * Integer.BYTES
        lock.lock()
        try {
            var newIndex = index + deltaWithinBlock
            while (newIndex >= COUNTER_SIZE_BYTES) {
                newIndex -= COUNTER_SIZE_BYTES
                blocksDelta++
            }
            while (newIndex < 0) {
                newIndex += COUNTER_SIZE_BYTES
                blocksDelta--
            }
            blocksDelta -= BLOCKS_AT_ONCE.toLong() // Compensate for the increment during nextBlock() below
            val addendDigits = ByteArray(counter!!.size)
            System.arraycopy(BinaryUtils.convertLongToBytes(blocksDelta), 0, addendDigits,
                    counter!!.size - java.lang.Long.BYTES, java.lang.Long.BYTES)
            if (blocksDelta < 0) {
                // Sign extend
                for (i in 0 until counter!!.size - java.lang.Long.BYTES) {
                    addendDigits[i] = -1
                }
            }
            var carry = false
            for (i in counter!!.indices) {
                val oldCounterUnsigned = if (counter!![i] < 0) counter!![i] + 256 else counter!![i]
                counter[i] += (addendDigits[counter!!.size - i - 1] + if (carry) 1 else 0).toByte()
                val newCounterUnsigned = if (counter!![i] < 0) counter!![i] + 256 else counter!![i]
                carry = oldCounterUnsigned > newCounterUnsigned || carry && oldCounterUnsigned == newCounterUnsigned
            }
            nextBlock()
            index = newIndex
        } finally {
            lock.unlock()
        }
    }

    companion object {

        private val serialVersionUID = 5949778642428995210L
        private val DEFAULT_SEED_SIZE_BYTES = 32
        /**
         * Theoretically, the Rijndael algorithm supports key sizes and block sizes of 16, 20, 24, 28 & 32
         * bytes. Thus, if Java contained a full implementation of Rijndael, specifying it would let us
         * support seeds of 16 to 32 and 36, 40, 44, 48, 52, 56, 60 & 64 bytes. However, neither Oracle
         * Java nor OpenJDK provides any implementation of the part of Rijndael that isn't AES.
         */
        private val ALGORITHM = "AES"
        private val ALGORITHM_MODE = "$ALGORITHM/ECB/NoPadding"
        /**
         * 128-bit counter. Package-visible for testing. Note to forkers: when running a cipher in ECB
         * mode, this counter's length should equal the cipher's block size.
         */
        internal val COUNTER_SIZE_BYTES = 16
        private val INTS_PER_BLOCK = COUNTER_SIZE_BYTES / Integer.BYTES
        /**
         * Number of blocks to encrypt at once, to construct/GC fewer arrays. This takes advantage of the
         * fact that in ECB mode, concatenating and then encrypting gives the same output as encrypting
         * and then concatenating, as long as both plaintexts are a whole number of blocks. (The AES block
         * size is 128 bits at all key lengths.)
         */
        private val BLOCKS_AT_ONCE = 16
        private val BYTES_AT_ONCE = COUNTER_SIZE_BYTES * BLOCKS_AT_ONCE
        private val HASH_ALGORITHM = "SHA-256"
        private val MAX_TOTAL_SEED_LENGTH_BYTES: Int
        private val ZEROES = ByteArray(COUNTER_SIZE_BYTES)
        /**
         * Returns the maximum length in bytes of an AES key, which is `Math.min(Cipher.getMaxAllowedKeyLength("AES/ECB/NoPadding") / 8, 32)`. If the seed is longer
         * than this, part of it becomes the counter's initial value. Otherwise, the full seed becomes the
         * AES key and the counter is initially zero.
         * @return the maximum length in bytes of an AES key.
         */
        var maxKeyLengthBytes = 0
            private set

        init {
            try {
                maxKeyLengthBytes = Cipher.getMaxAllowedKeyLength(ALGORITHM_MODE) / 8
            } catch (e: GeneralSecurityException) {
                throw RuntimeException(e)
            }

            LoggerFactory.getLogger(AesCounterRandom::class.java)
                    .info("Maximum allowed key length for AES is {} bytes", maxKeyLengthBytes)
            maxKeyLengthBytes = Math.min(maxKeyLengthBytes, 32)
            MAX_TOTAL_SEED_LENGTH_BYTES = maxKeyLengthBytes + COUNTER_SIZE_BYTES
        }

        private fun getKeyLength(input: ByteArray): Int {
            return if (input.size > maxKeyLengthBytes)
                maxKeyLengthBytes
            else
                if (input.size >= 24) 24 else 16
        }
    }
}
/**
 * Creates a new RNG and seeds it using 256 bits from the [DefaultSeedGenerator].
 * @throws SeedException if the [DefaultSeedGenerator] fails to generate a seed.
 */
