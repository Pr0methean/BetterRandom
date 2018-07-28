package io.github.pr0methean.betterrandom.prng.adapter

import io.github.pr0methean.betterrandom.util.BinaryUtils.convertBytesToLong

import com.google.common.base.MoreObjects.ToStringHelper
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator
import io.github.pr0methean.betterrandom.seed.RandomSeederThread
import io.github.pr0methean.betterrandom.seed.SeedException
import io.github.pr0methean.betterrandom.seed.SeedGenerator
import io.github.pr0methean.betterrandom.util.BinaryUtils
import java.io.IOException
import java.io.ObjectInputStream
import java.util.SplittableRandom
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe PRNG that wraps a [ThreadLocal]&lt;[SplittableRandom]&gt;. Reseeding this
 * will only affect the calling thread, so this can't be used with a [RandomSeederThread].
 * Instead, use a [ReseedingSplittableRandomAdapter].
 * @author Chris Hennick
 */
class SplittableRandomAdapter : DirectSplittableRandomAdapter {
    @Transient
    private var splittableRandoms: ThreadLocal<SplittableRandom>? = null
    @Transient
    private var entropyBits: ThreadLocal<AtomicLong>? = null
    @Transient
    private var seeds: ThreadLocal<ByteArray>? = null

    protected override val splittableRandom: SplittableRandom
        get() = splittableRandoms!!.get()

    override val seed: ByteArray
        get() = seeds!!.get().clone()

    /**
     * Use the provided seed generation strategy to create the seed for the master [ ], which will be split to generate an instance for each thread.
     * @param seedGenerator The seed generation strategy that will provide the seed value for this
     * RNG.
     * @throws SeedException if there is a problem generating a seed.
     */
    @Throws(SeedException::class)
    constructor(seedGenerator: SeedGenerator) : this(seedGenerator.generateSeed(java.lang.Long.BYTES)) {
    }

    /**
     * Use the provided seed for the master [SplittableRandom], which will be split to generate
     * an instance for each thread.
     * @param seed The seed. Must be 8 bytes.
     */
    @JvmOverloads constructor(seed: ByteArray = DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(java.lang.Long.BYTES)) : super(seed) {
        initSubclassTransientFields()
    }

    /**
     * Use the provided seed for the master [SplittableRandom], which will be split to generate
     * an instance for each thread.
     * @param seed The seed.
     */
    constructor(seed: Long) : super(seed) {
        initSubclassTransientFields()
    }

    override fun useParallelStreams(): Boolean {
        return true
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(`in`: ObjectInputStream) {
        `in`.defaultReadObject()
        initSubclassTransientFields()
    }

    /** Returns the entropy count for the calling thread (it is separate for each thread).  */
    override fun getEntropyBits(): Long {
        return entropyBits!!.get().get()
    }

    override fun debitEntropy(bits: Long) {
        entropyBits!!.get().addAndGet(-bits)
    }

    override fun creditEntropyForNewSeed(seedLength: Int) {
        if (entropyBits != null) {
            entropyBits!!.get().updateAndGet(
                    { oldCount -> Math.max(oldCount, Math.min(seedLength, newSeedLength) * 8L) })
        }
    }

    private fun initSubclassTransientFields() {
        lock.lock()
        try {
            splittableRandoms = ThreadLocal.withInitial {
                // Necessary because SplittableRandom.split() isn't itself thread-safe.
                lock.lock()
                try {
                    return@ThreadLocal.withInitial underlying . split ()
                } finally {
                    lock.unlock()
                }
            }
            entropyBits = ThreadLocal.withInitial { AtomicLong(SEED_LENGTH_BITS.toLong()) }
            // getSeed() will return the master seed on each thread where setSeed() hasn't yet been called
            seeds = ThreadLocal.withInitial { seed }
        } finally {
            lock.unlock()
        }
    }

    override fun addSubclassFields(original: ToStringHelper): ToStringHelper {
        return original.add("splittableRandoms", splittableRandoms!!)
    }

    /**
     * Not supported, because this class uses a thread-local seed.
     * @param seedGenerator ignored.
     * @throws UnsupportedOperationException always.
     */
    override fun setSeedGenerator(seedGenerator: SeedGenerator?) {
        throw UnsupportedOperationException("Use ReseedingSplittableRandomAdapter instead")
    }

    /**
     * {@inheritDoc} Applies only to the calling thread.
     */
    override fun setSeed(seed: ByteArray) {
        if (seed.size != java.lang.Long.BYTES) {
            throw IllegalArgumentException("SplittableRandomAdapter requires an 8-byte seed")
        }
        setSeed(convertBytesToLong(seed))
    }

    /**
     * {@inheritDoc} Applies only to the calling thread.
     */
    override fun setSeed(
            seed: Long) {
        if (this.seed == null) {
            super.setSeed(seed)
        }
        if (splittableRandoms != null) {
            splittableRandoms!!.set(SplittableRandom(seed))
            if (entropyBits != null) {
                entropyBits!!.get().updateAndGet({ oldValue -> Math.max(oldValue, SEED_LENGTH_BITS.toLong()) })
            }
            if (seeds != null) {
                seeds!!.set(BinaryUtils.convertLongToBytes(seed))
            }
        }
    }

    companion object {

        private val SEED_LENGTH_BITS = java.lang.Long.BYTES * 8
        private val serialVersionUID = 2190439512972880590L
    }
}
/**
 * Use the [DefaultSeedGenerator] to generate a seed for the master [ ], which will be split to generate an instance for each thread.
 * @throws SeedException if the [DefaultSeedGenerator] fails to generate a seed.
 */
