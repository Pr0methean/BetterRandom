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
import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom
import io.github.pr0methean.betterrandom.EntropyCountingRandom
import io.github.pr0methean.betterrandom.RepeatableRandom
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator
import io.github.pr0methean.betterrandom.seed.SeedException
import io.github.pr0methean.betterrandom.seed.SeedGenerator
import io.github.pr0methean.betterrandom.util.BinaryUtils
import io.github.pr0methean.betterrandom.util.Dumpable
import io.github.pr0methean.betterrandom.util.EntryPoint
import java.security.SecureRandom
import java.util.Random

/**
 *
 * Wraps any [Random] as a [RepeatableRandom] and [ByteArrayReseedableRandom].
 * Can be used to encapsulate away a change of implementation in midstream.
 *
 * Caution: This depends on the underlying [Random] for thread-safety. When used with a
 * vanilla [Random], this means that its output for the same seed will vary when accessed
 * concurrently from multiple threads, if the calls include e.g. [.nextLong],
 * [.nextGaussian] or [.nextDouble]. However, [.nextInt] will still be
 * transactional.
 * @author Chris Hennick
 */
open class RandomWrapper : BaseRandom {
    @Volatile
    private var wrapped: Random? = null
    @Volatile
    private var unknownSeed = true
    private var haveParallelStreams: Boolean = false

    /**
     * Returns the wrapped PRNG's seed, if we know it. When this RandomWrapper is wrapping a passed-in
     * [Random] that's not a [RepeatableRandom], we won't know the seed until the next
     * [.setSeed] or [.setSeed] call lets us set it ourselves, and so an
     * [UnsupportedOperationException] will be thrown until then.
     * @throws UnsupportedOperationException if this RandomWrapper doesn't know the wrapped PRNG's
     * seed.
     */
    override val seed: ByteArray
        get() {
            if (unknownSeed) {
                throw UnsupportedOperationException()
            }
            return super.getSeed()
        }

    override// can't use a seed yet
    val newSeedLength: Int
        get() {
            if (lock == null) {
                return 0
            }
            lock.lock()
            try {
                if (wrapped == null) {
                    return 0
                }
                return if (wrapped is ByteArrayReseedableRandom)
                    (wrapped as ByteArrayReseedableRandom)
                            .newSeedLength
                else
                    java.lang.Long.BYTES
            } finally {
                lock.unlock()
            }
        }

    /**
     * Wraps a [Random] and seeds it using the provided seedArray generation strategy.
     * @param seedGenerator The seedArray generation strategy that will provide the seedArray
     * value for this RNG.
     * @throws SeedException If there is a problem generating a seedArray.
     */
    @EntryPoint @Throws(SeedException::class)
    constructor(seedGenerator: SeedGenerator) : super(seedGenerator, java.lang.Long.BYTES) {
        wrapped = Random(BinaryUtils.convertBytesToLong(seed))
        unknownSeed = false
        haveParallelStreams = wrapped!!.longs().isParallel
    }

    /**
     * Wraps a new [Random] seeded it with the specified seed data.
     * @param seed 8 bytes of seed data used to initialise the RNG.
     */
    @JvmOverloads constructor(seed: ByteArray = DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(java.lang.Long.BYTES)) : super(seed) {
        if (seed.size != java.lang.Long.BYTES) {
            throw IllegalArgumentException(
                    "RandomWrapper requires an 8-byte seed when defaulting to java.util.Random")
        }
        wrapped = Random(BinaryUtils.convertBytesToLong(seed))
        unknownSeed = false
        haveParallelStreams = wrapped!!.longs().isParallel
    }

    /**
     * Wraps a new [Random] seeded with the specified seed.
     * @param seed seed used to initialise the [Random].
     */
    @EntryPoint constructor(seed: Long) : super(seed) {
        wrapped = Random(seed)
        unknownSeed = false
        haveParallelStreams = wrapped!!.longs().isParallel
    }

    /**
     * Creates an instance wrapping the given [Random].
     * @param wrapped The [Random] to wrap.
     */
    @EntryPoint constructor(wrapped: Random) : super(getSeedOrDummy(wrapped)) {
        unknownSeed = wrapped !is RepeatableRandom
        readEntropyOfWrapped(wrapped)
        this.wrapped = wrapped
        haveParallelStreams = wrapped.longs().isParallel
    }// We won't know the wrapped PRNG's seed

    override fun toString(): String {
        return String.format("RandomWrapper (currently around %s)", wrapped)
    }

    override fun useParallelStreams(): Boolean {
        return haveParallelStreams
    }

    override fun next(bits: Int): Int {
        return if (bits >= 32)
            getWrapped()!!.nextInt()
        else if (bits == 31)
            getWrapped()!!.nextInt().ushr(1)
        else
            getWrapped()!!.nextInt(1 shl bits)
    }

    /**
     * Returns the [Random] instance this RandomWrapper is currently wrapping.
     * @return The wrapped [Random].
     */
    @EntryPoint
    open fun getWrapped(): Random? {
        lock.lock()
        try {
            return wrapped
        } finally {
            lock.unlock()
        }
    }

    /**
     * Replaces the underlying [Random] instance with the given one on subsequent calls.
     * @param wrapped The new [Random] instance to wrap.
     */
    @EntryPoint
    fun setWrapped(wrapped: Random) {
        lock.lock()
        try {
            this.wrapped = wrapped
            readEntropyOfWrapped(wrapped)
            seed = getSeedOrDummy(wrapped)
            unknownSeed = wrapped !is RepeatableRandom
            haveParallelStreams = wrapped.longs().isParallel
        } finally {
            lock.unlock()
        }
    }

    private fun readEntropyOfWrapped(wrapped: Random) {
        entropyBits.set(if (wrapped is EntropyCountingRandom)
            (wrapped as EntropyCountingRandom)
                    .entropyBits
        else
            if (wrapped is RepeatableRandom)
                (wrapped as RepeatableRandom).seed.size * java.lang.Byte.SIZE.toLong()
            else
                java.lang.Long.SIZE)
    }

    override fun addSubclassFields(original: ToStringHelper): ToStringHelper {
        return original.add("wrapped",
                if (wrapped is Dumpable) (wrapped as Dumpable).dump() else wrapped)
    }

    override fun setSeed(seed: Long) {
        var locked = false
        if (lock != null) {
            lock.lock()
            locked = true
        }
        try {
            if (wrapped != null) {
                wrapped!!.setSeed(seed)
                super.setSeedInternal(BinaryUtils.convertLongToBytes(seed))
                unknownSeed = false
            }
        } finally {
            if (locked) {
                lock.unlock()
            }
        }
    }

    /**
     * Delegates to one of [ByteArrayReseedableRandom.setSeed], [ ][SecureRandom.setSeed] or [Random.setSeed].
     * @param seed The new seed.
     */
    override fun setSeedInternal(seed: ByteArray?) {
        if (seed == null) {
            throw IllegalArgumentException("Seed must not be null")
        }
        var locked = false
        if (lock != null) {
            lock.lock()
            locked = true
        }
        try {
            if (this.seed == null || this.seed!!.size != seed.size) {
                this.seed = ByteArray(seed.size)
            }
            super.setSeedInternal(seed)
            if (wrapped == null) {
                return
            }
            var asByteArrayReseedable: ByteArrayReseedableRandom? = null
            if (wrapped is ByteArrayReseedableRandom) {
                asByteArrayReseedable = wrapped as ByteArrayReseedableRandom?
                if (asByteArrayReseedable!!.preferSeedWithLong() && seed.size == java.lang.Long.BYTES) {
                    asByteArrayReseedable = null
                }
            } else if (wrapped is SecureRandom) {
                // Special handling, since SecureRandom isn't ByteArrayReseedableRandom but does have
                // setSeed(byte[])
                (wrapped as SecureRandom).setSeed(seed)
                unknownSeed = false
                return
            } else if (seed.size != java.lang.Long.BYTES) {
                throw IllegalArgumentException(
                        "RandomWrapper requires an 8-byte seed when not wrapping a ByteArrayReseedableRandom")
            }
            if (asByteArrayReseedable != null) {
                asByteArrayReseedable.setSeed(seed)
                unknownSeed = false
            } else {
                wrapped!!.setSeed(BinaryUtils.convertBytesToLong(seed))
                unknownSeed = false
            }
        } finally {
            if (locked) {
                lock.unlock()
            }
        }
    }

    override fun preferSeedWithLong(): Boolean {
        if (lock == null) {
            return false // safe default
        }
        lock.lock()
        try {
            val currentWrapped = getWrapped()
            return currentWrapped !is ByteArrayReseedableRandom || (currentWrapped as ByteArrayReseedableRandom).preferSeedWithLong()
        } finally {
            lock.unlock()
        }
    }

    override fun nextBytes(bytes: ByteArray) {
        getWrapped()!!.nextBytes(bytes)
        debitEntropy(bytes.size * java.lang.Byte.SIZE.toLong())
    }

    override fun nextInt(): Int {
        val result = getWrapped()!!.nextInt()
        debitEntropy(Integer.SIZE.toLong())
        return result
    }

    override fun nextInt(bound: Int): Int {
        val result = getWrapped()!!.nextInt(bound)
        debitEntropy(BaseRandom.entropyOfInt(0, bound).toLong())
        return result
    }

    override fun nextLongNoEntropyDebit(): Long {
        return getWrapped()!!.nextLong()
    }

    override fun nextBoolean(): Boolean {
        val result = getWrapped()!!.nextBoolean()
        debitEntropy(1)
        return result
    }

    override fun nextFloat(): Float {
        val result = getWrapped()!!.nextFloat()
        debitEntropy(BaseRandom.ENTROPY_OF_FLOAT.toLong())
        return result
    }

    public override fun nextDoubleNoEntropyDebit(): Double {
        return getWrapped()!!.nextDouble()
    }

    override fun nextGaussian(): Double {
        val result = getWrapped()!!.nextGaussian()

        // Upper bound. 2 Gaussians are generated from 2 nextDouble calls, which once made are either
        // used or rerolled.
        debitEntropy(BaseRandom.ENTROPY_OF_DOUBLE.toLong())

        return result
    }

    companion object {

        protected val DUMMY_SEED = ByteArray(8)
        private val serialVersionUID = -6526304552538799385L

        private fun getSeedOrDummy(wrapped: Random): ByteArray {
            return if (wrapped is RepeatableRandom)
                (wrapped as RepeatableRandom).seed
            else
                DUMMY_SEED
        }
    }
}
/**
 * Wraps a [Random] and seeds it using the default seeding strategy.
 * @throws SeedException if any.
 */
