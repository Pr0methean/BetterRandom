package io.github.pr0methean.betterrandom.prng

import com.google.common.base.MoreObjects.ToStringHelper
import io.github.pr0methean.betterrandom.seed.SeedException
import io.github.pr0methean.betterrandom.seed.SeedGenerator
import io.github.pr0methean.betterrandom.util.BinaryUtils
import java.io.IOException
import java.io.ObjectInputStream
import java.io.Serializable
import java.util.Random
import java.util.function.Function
import java.util.function.LongFunction
import java.util.function.Supplier

/**
 * Wraps a [ThreadLocal]&lt;[BaseRandom]&gt; in order to provide concurrency that most
 * implementations of [BaseRandom] can't implement naturally.
 */
open class ThreadLocalRandomWrapper : RandomWrapper {
    protected val initializer: Supplier<out BaseRandom>
    private val explicitSeedSize: Int?
    @Transient
    protected var threadLocal: ThreadLocal<BaseRandom>? = null

    override val seed: ByteArray
        get() = wrapped!!.getSeed()

    override val entropyBits: Long
        get() = wrapped!!.getEntropyBits()

    override val newSeedLength: Int
        get() = if (threadLocal == null)
            0
        else
            explicitSeedSize ?: wrapped!!.newSeedLength

    /**
     * Wraps the given [Supplier]. This ThreadLocalRandomWrapper will be serializable if the
     * [Supplier] is serializable.
     * @param initializer a supplier that will be called to provide the initial [BaseRandom]
     * for each thread.
     */
    @Throws(SeedException::class)
    constructor(initializer: Supplier<out BaseRandom>) : super(0) {
        this.initializer = initializer
        threadLocal = ThreadLocal.withInitial(initializer)
        explicitSeedSize = null
    }

    /**
     * Wraps a seed generator and a function that takes a seed byte array as input. This
     * ThreadLocalRandomWrapper will be serializable if the [Function] is serializable.
     * @param seedSize the size of seed arrays to generate.
     * @param seedGenerator The seed generation strategy that will provide the seed value for each
     * thread's [BaseRandom].
     * @param creator a [Function] that creates a [BaseRandom] from each seed.
     * Probably a constructor reference.
     */
    @Throws(SeedException::class)
    constructor(seedSize: Int, seedGenerator: SeedGenerator,
                creator: Function<ByteArray, out BaseRandom>) : super(0) {
        explicitSeedSize = seedSize
        initializer = {
            creator
                    .apply(seedGenerator.generateSeed(seedSize))
        } as Serializable
        threadLocal = ThreadLocal.withInitial(initializer)
    }

    /**
     * Not supported, because this class uses a thread-local seed.
     * @param seedGenerator ignored.
     * @throws UnsupportedOperationException always.
     */
    override fun setSeedGenerator(seedGenerator: SeedGenerator?) {
        throw UnsupportedOperationException("This can't be reseeded by a RandomSeederThread")
    }

    override fun withProbabilityInternal(probability: Double): Boolean {
        return wrapped!!.withProbabilityInternal(probability)
    }

    override fun nextLong(bound: Long): Long {
        return wrapped!!.nextLong(bound)
    }

    override fun nextInt(origin: Int, bound: Int): Int {
        return wrapped!!.nextInt(origin, bound)
    }

    override fun nextLong(origin: Long, bound: Long): Long {
        return wrapped!!.nextLong(origin, bound)
    }

    override fun getWrapped(): BaseRandom? {
        return threadLocal!!.get()
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(`in`: ObjectInputStream) {
        `in`.defaultReadObject()
        threadLocal = ThreadLocal.withInitial(initializer)
    }

    override fun nextBytes(bytes: ByteArray) {
        wrapped!!.nextBytes(bytes)
    }

    override fun nextInt(): Int {
        return wrapped!!.nextInt()
    }

    override fun nextInt(bound: Int): Int {
        return wrapped!!.nextInt(bound)
    }

    override fun nextLongNoEntropyDebit(): Long {
        return wrapped!!.nextLongNoEntropyDebit()
    }

    override fun nextBoolean(): Boolean {
        return wrapped!!.nextBoolean()
    }

    override fun nextFloat(): Float {
        return wrapped!!.nextFloat()
    }

    override fun nextDoubleNoEntropyDebit(): Double {
        return wrapped!!.nextDoubleNoEntropyDebit()
    }

    override fun nextGaussian(): Double {
        return wrapped!!.nextGaussian()
    }

    override fun useParallelStreams(): Boolean {
        return true
    }

    override fun addSubclassFields(original: ToStringHelper): ToStringHelper {
        return original.add("wrapped on this thread", wrapped!!.dump())
    }

    override fun preferSeedWithLong(): Boolean {
        val newSeedLength = newSeedLength
        return newSeedLength > 0 && newSeedLength <= java.lang.Long.BYTES
    }

    @Synchronized
    override fun setSeed(seed: Long) {
        if (threadLocal != null) {
            val wrapped = wrapped
            wrapped!!.setSeed(seed)
            wrapped.creditEntropyForNewSeed(java.lang.Long.BYTES)
        }
    }

    override fun setSeedInternal(seed: ByteArray?) {
        if (seed == null) {
            throw IllegalArgumentException("Seed must not be null")
        }
        if (threadLocal != null) {
            val wrapped = wrapped
            wrapped!!.setSeed(seed)
            wrapped.creditEntropyForNewSeed(seed.size)
        }
        if (this.seed == null) {
            this.seed = seed.clone() // Needed for serialization
        }
    }

    override fun debitEntropy(bits: Long) {
        wrapped!!.debitEntropy(bits)
    }

    companion object {

        private val serialVersionUID = 1199235201518562359L

        /**
         * Uses this class and [RandomWrapper] to decorate any implementation of [Random] that
         * can be constructed from a `long` seed into a fully-concurrent one.
         * @param legacyCreator a function that provides the [Random] that underlies the
         * returned wrapper on each thread, taking a seed as input.
         * @param seedGenerator the seed generator whose output will be fed to `legacyCreator`.
         * @return a ThreadLocalRandomWrapper decorating instances created by `legacyCreator`.
         */
        fun wrapLegacy(legacyCreator: LongFunction<Random>,
                       seedGenerator: SeedGenerator): ThreadLocalRandomWrapper {
            return ThreadLocalRandomWrapper(java.lang.Long.BYTES, seedGenerator,
                    { bytes -> RandomWrapper(legacyCreator.apply(BinaryUtils.convertBytesToLong(bytes))) })
        }
    }
}
