package io.github.pr0methean.betterrandom.prng

import com.google.common.base.MoreObjects.ToStringHelper
import io.github.pr0methean.betterrandom.SeekableRandom
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator
import io.github.pr0methean.betterrandom.seed.SeedException
import io.github.pr0methean.betterrandom.seed.SeedGenerator
import io.github.pr0methean.betterrandom.util.BinaryUtils
import io.github.pr0methean.betterrandom.util.EntryPoint
import java.io.IOException
import java.io.ObjectOutputStream
import java.util.concurrent.atomic.AtomicLong

/**
 *
 * From the original description, "PCG is a family of simple fast space-efficient statistically
 * good algorithms for random number generation. Unlike many general-purpose RNGs, they are also
 * hard to predict." This is a Java port of the "XSH RR 64/32" generator presented at [http://www.pcg-random.org/](http://www.pcg-random.org/). Period is 2<sup>62</sup> bits.
 * This PRNG is seekable.
 *
 *
 * Sharing a single instance across threads that are frequently using it concurrently isn't
 * recommended unless memory is too constrained to use with a [ThreadLocalRandomWrapper].
 *
 * @author M.E. O'Neill (algorithm and C++ implementation)
 * @author Chris Hennick (Java port)
 */
class Pcg64Random : BaseRandom, SeekableRandom {

    private val internal: AtomicLong?

    override val seed: ByteArray
        get() = BinaryUtils.convertLongToBytes(internal!!.get()).clone()

    override val newSeedLength: Int
        get() = java.lang.Long.BYTES

    constructor() : this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR) {}

    @EntryPoint @Throws(SeedException::class)
    constructor(seedGenerator: SeedGenerator) : this(seedGenerator.generateSeed(java.lang.Long.BYTES)) {
    }

    @EntryPoint constructor(seed: ByteArray) : super(seed) {
        if (seed.size != java.lang.Long.BYTES) {
            throw IllegalArgumentException("Pcg64Random requires an 8-byte seed")
        }
        internal = AtomicLong(BinaryUtils.convertBytesToLong(seed))
    }

    @EntryPoint constructor(seed: Long) : super(seed) {
        internal = AtomicLong(seed)
    }

    override fun nextLongNoEntropyDebit(): Long {
        lock.lock()
        try {
            return (next(32).toLong() shl 32) + next(32)
        } finally {
            lock.unlock()
        }
    }

    override fun setSeed(seed: Long) {
        if (internal != null) {
            lock.lock()
            try {
                internal.set(seed)
                creditEntropyForNewSeed(java.lang.Long.BYTES)
            } finally {
                lock.unlock()
            }
        }
    }

    override fun advance(delta: Long) {
        var delta = delta
        if (delta == 0L) {
            return
        }
        // The method used here is based on Brown, "Random Number Generation
        // with Arbitrary Stride,", Transactions of the American Nuclear
        // Society (Nov. 1994).  The algorithm is very similar to fast
        // exponentiation.
        var curMult = MULTIPLIER
        var curPlus = INCREMENT
        var accMult: Long = 1
        var accPlus: Long = 0
        while (delta != 0L) {
            if (delta and 1 == 1L) {
                accMult *= curMult
                accPlus = accPlus * curMult + curPlus
            }
            curPlus = (curMult + 1) * curPlus
            curMult *= curMult
            delta = delta ushr 1
        }
        val finalAccMult = accMult
        val finalAccPlus = accPlus
        lock.lock()
        try {
            internal!!.updateAndGet { old -> finalAccMult * old + finalAccPlus }
        } finally {
            lock.unlock()
        }
    }

    public override fun setSeedInternal(seed: ByteArray?) {
        super.setSeedInternal(seed)
        if (seed!!.size != java.lang.Long.BYTES) {
            throw IllegalArgumentException("Pcg64Random requires an 8-byte seed")
        }
        if (internal != null) {
            lock.lock()
            try {
                internal.set(BinaryUtils.convertBytesToLong(seed))
            } finally {
                lock.unlock()
            }
        }
    }

    override fun next(bits: Int): Int {
        var oldInternal: Long
        var newInternal: Long
        do {
            oldInternal = internal!!.get()
            newInternal = oldInternal * MULTIPLIER + INCREMENT
        } while (!internal!!.weakCompareAndSet(oldInternal, newInternal))
        // Calculate output function (XSH RR), uses old state for max ILP
        val xorshifted = (oldInternal.ushr(ROTATION1) xor oldInternal).ushr(ROTATION2).toInt()
        val rot = oldInternal.ushr(ROTATION3).toInt()
        return (xorshifted.ushr(rot) or (xorshifted shl (-rot and MASK))).ushr(Integer.SIZE - bits)
    }

    override fun addSubclassFields(original: ToStringHelper): ToStringHelper {
        return original.add("internal", internal!!.get())
    }

    override fun nextGaussian(): Double {
        lock.lock()
        try {
            return super.nextGaussian()
        } finally {
            lock.unlock()
        }
    }

    @Throws(IOException::class)
    private fun writeObject(out: ObjectOutputStream) {
        // Copy the long seed back to the array seed
        System.arraycopy(BinaryUtils.convertLongToBytes(internal!!.get()), 0, seed!!, 0, java.lang.Long.BYTES)
        out.defaultWriteObject()
    }

    companion object {

        private val serialVersionUID = 1677405697790847137L
        private val MULTIPLIER = 6364136223846793005L
        private val INCREMENT = 1442695040888963407L
        private val WANTED_OP_BITS = 5
        private val ROTATION1 = (WANTED_OP_BITS + Integer.SIZE) / 2
        private val ROTATION2 = java.lang.Long.SIZE - Integer.SIZE - WANTED_OP_BITS
        private val ROTATION3 = java.lang.Long.SIZE - WANTED_OP_BITS
        private val MASK = (1 shl WANTED_OP_BITS) - 1
    }
}
