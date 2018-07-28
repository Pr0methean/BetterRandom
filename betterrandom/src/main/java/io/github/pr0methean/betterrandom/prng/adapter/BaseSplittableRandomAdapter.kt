package io.github.pr0methean.betterrandom.prng.adapter

import io.github.pr0methean.betterrandom.prng.BaseRandom
import java.util.SplittableRandom
import java.util.function.DoubleSupplier

/**
 * Abstract class for implementations of [BaseRandom] that wrap one or more [ ] instances.
 * @author Chris Hennick
 */
abstract class BaseSplittableRandomAdapter : BaseRandom {

    /**
     * Returns the [SplittableRandom] that is to be used to generate random numbers for the
     * current thread. ([SplittableRandom] isn't thread-safe.) Called by all the `next*`
     * methods.
     * @return the [SplittableRandom] to use with the current thread.
     */
    protected abstract val splittableRandom: SplittableRandom

    /** Returns the only supported seed length.  */
    override val newSeedLength: Int
        get() = java.lang.Long.BYTES

    /**
     * Constructs an instance with the given seed.
     * @param seed The seed.
     */
    protected constructor(seed: ByteArray) : super(seed) {}

    /**
     * Constructs an instance with the given seed.
     * @param seed The seed.
     */
    protected constructor(seed: Long) : super(seed) {}

    /** Delegates to [SplittableRandom.nextDouble(bound)][SplittableRandom.nextDouble].  */
    override fun nextDouble(bound: Double): Double {
        val out = splittableRandom.nextDouble(bound)
        debitEntropy(BaseRandom.ENTROPY_OF_DOUBLE.toLong())
        return out
    }

    /**
     * Delegates to [SplittableRandom.nextDouble(origin,][SplittableRandom.nextDouble].
     */
    override fun nextDouble(origin: Double, bound: Double): Double {
        val out = splittableRandom.nextDouble(origin, bound)
        debitEntropy(BaseRandom.ENTROPY_OF_DOUBLE.toLong())
        return out
    }

    /** Delegates to [SplittableRandom.nextInt] or [SplittableRandom.nextInt].  */
    override fun next(bits: Int): Int {
        debitEntropy(bits.toLong())
        return if (bits >= 32)
            splittableRandom.nextInt()
        else if (bits == 31)
            splittableRandom.nextInt().ushr(1)
        else
            splittableRandom.nextInt(1 shl bits)
    }

    /** Delegates to [SplittableRandom.nextInt(256)][SplittableRandom.nextInt].  */
    override fun nextBytes(
            bytes: ByteArray) {
        val local = splittableRandom
        for (i in bytes.indices) {
            bytes[i] = local.nextInt(256).toByte()
            debitEntropy(java.lang.Byte.SIZE.toLong())
        }
    }

    /** Delegates to [SplittableRandom.nextInt].  */
    override fun nextInt(): Int {
        val out = splittableRandom.nextInt()
        debitEntropy(Integer.SIZE.toLong())
        return out
    }

    /** Delegates to [SplittableRandom.nextInt(bound)][SplittableRandom.nextInt].  */
    override fun nextInt(bound: Int): Int {
        val out = splittableRandom.nextInt(bound)
        debitEntropy(BaseRandom.entropyOfInt(0, bound).toLong())
        return out
    }

    /**
     * Delegates to [SplittableRandom.nextInt(origin,][SplittableRandom.nextInt].
     */
    override fun nextInt(origin: Int, bound: Int): Int {
        val out = splittableRandom.nextInt(origin, bound)
        debitEntropy(BaseRandom.entropyOfInt(origin, bound).toLong())
        return out
    }

    /**
     *
     *Delegates to [SplittableRandom.nextDouble]. {@inheritDoc}  */
    override fun withProbabilityInternal(probability: Double): Boolean {
        val result = splittableRandom.nextDouble() < probability
        // We're only outputting one bit
        debitEntropy(1)
        return result
    }

    override fun preferSeedWithLong(): Boolean {
        return true
    }

    /**
     *
     *Delegates to [SplittableRandom.nextLong]. {@inheritDoc}  */
    override fun nextLongNoEntropyDebit(): Long {
        return splittableRandom.nextLong()
    }

    /** Delegates to [SplittableRandom.nextLong(bound)][SplittableRandom.nextLong].  */
    override fun nextLong(bound: Long): Long {
        val out = splittableRandom.nextLong(bound)
        debitEntropy(BaseRandom.entropyOfLong(0, bound).toLong())
        return out
    }

    /**
     * Delegates to [SplittableRandom.nextLong(origin,][SplittableRandom.nextLong].
     */
    override fun nextLong(origin: Long, bound: Long): Long {
        val out = splittableRandom.nextLong(origin, bound)
        debitEntropy(BaseRandom.entropyOfLong(origin, bound).toLong())
        return out
    }

    /** Delegates to [SplittableRandom.nextDouble].  */
    override fun nextDoubleNoEntropyDebit(): Double {
        return splittableRandom.nextDouble()
    }

    /**
     * Delegates to [SplittableRandom.nextDouble] via [.internalNextGaussian].
     */
    override fun nextGaussian(): Double {
        // Upper bound. 2 Gaussians are generated from 2 nextDouble calls, which once made are either
        // used or rerolled.
        debitEntropy(BaseRandom.ENTROPY_OF_DOUBLE.toLong())

        return internalNextGaussian { splittableRandom.nextDouble() }
    }

    override fun lockForNextGaussian() {
        // No-op.
    }

    override fun unlockForNextGaussian() {
        // No-op.
    }

    /** Delegates to [SplittableRandom.nextBoolean].  */
    override fun nextBoolean(): Boolean {
        val out = splittableRandom.nextBoolean()
        debitEntropy(1)
        return out
    }

    /** Delegates to [SplittableRandom.nextInt].  */
    override fun nextFloat(): Float {
        val out = splittableRandom.nextInt(1 shl BaseRandom.ENTROPY_OF_FLOAT) / (1 shl BaseRandom.ENTROPY_OF_FLOAT).toFloat()
        debitEntropy(BaseRandom.ENTROPY_OF_FLOAT.toLong())
        return out
    }

    companion object {

        private val serialVersionUID = 4273652147052638879L
    }
}
