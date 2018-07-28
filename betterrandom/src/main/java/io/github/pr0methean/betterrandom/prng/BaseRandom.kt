package io.github.pr0methean.betterrandom.prng

import com.google.common.base.MoreObjects
import com.google.common.base.MoreObjects.ToStringHelper
import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom
import io.github.pr0methean.betterrandom.EntropyCountingRandom
import io.github.pr0methean.betterrandom.RepeatableRandom
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator
import io.github.pr0methean.betterrandom.seed.RandomSeederThread
import io.github.pr0methean.betterrandom.seed.SeedException
import io.github.pr0methean.betterrandom.seed.SeedGenerator
import io.github.pr0methean.betterrandom.util.BinaryUtils
import io.github.pr0methean.betterrandom.util.Dumpable
import io.github.pr0methean.betterrandom.util.EntryPoint
import java.io.IOException
import java.io.InvalidObjectException
import java.io.ObjectInputStream
import java.util.Random
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import java.util.function.DoubleSupplier
import java.util.stream.BaseStream
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Abstract [Random] with a seed field and an implementation of entropy counting.
 * @author Chris Hennick
 */
abstract class BaseRandom
/**
 * Creates a new RNG with the provided seed.
 * @param seed the seed.
 */
protected constructor(seed: ByteArray?) : Random(), ByteArrayReseedableRandom, RepeatableRandom, Dumpable, EntropyCountingRandom {
    /**
     * If the referent is non-null, it will be invoked to reseed this PRNG whenever random output is
     * taken and [.getEntropyBits] called immediately afterward would return zero or
     * negative.
     */
    protected val seedGenerator = AtomicReference<SeedGenerator>(null)
    /** Lock to prevent concurrent modification of the RNG's internal state.  */
    protected val lock = ReentrantLock()
    /** Stores the entropy estimate backing [.getEntropyBits].  */
    protected val entropyBits_ = AtomicLong(0)
    // Stored as a long since there's no atomic double
    private val nextNextGaussian = AtomicLong(NAN_LONG_BITS)
    /**
     * The seed this PRNG was seeded with, as a byte array. Used by [.getSeed] even if the
     * actual internal state of the PRNG is stored elsewhere (since otherwise getSeed() would require
     * a slow type conversion).
     */
    protected var seed_: ByteArray? = null
    /**
     * Set by the constructor once either [Random.Random] or [Random.Random] has
     * returned. Intended for [.setSeed], which may have to ignore calls while this is
     * false if the subclass does not support 8-byte seeds, or it overriddes setSeed(long) to use
     * subclass fields.
     */
    @Transient
    protected var superConstructorFinished = false

    override val seed: ByteArray
        get() {
            lock.lock()
            try {
                return seed_!!.clone()
            } finally {
                lock.unlock()
            }
        }

    override val entropyBits: Long
        get() = entropyBits_.get()

    abstract override val newSeedLength: Int

    /**
     * Seed the RNG using the [DefaultSeedGenerator] to create a seed of the specified size.
     * @param seedSizeBytes The number of bytes to use for seed data.
     * @throws SeedException if the [DefaultSeedGenerator] fails to generate a seed.
     */
    @Throws(SeedException::class)
    protected constructor(seedSizeBytes: Int) : this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(seedSizeBytes)) {
    }

    /**
     * Creates a new RNG and seeds it using the provided seed generation strategy.
     * @param seedGenerator The seed generation strategy that will provide the seed value for this
     * RNG.
     * @param seedLength The seed length in bytes.
     * @throws SeedException If there is a problem generating a seed.
     */
    @Throws(SeedException::class)
    protected constructor(seedGenerator: SeedGenerator, seedLength: Int) : this(seedGenerator.generateSeed(seedLength)) {
    }

    init {
        if (seed == null) {
            throw IllegalArgumentException("Seed must not be null")
        }
        initTransientFields()
        setSeedInternal(seed)
    }

    /**
     * Creates a new RNG with the provided seed. Only works in subclasses that can accept an 8-byte or
     * shorter seed.
     * @param seed the seed.
     */
    protected constructor(seed: Long) : this(BinaryUtils.convertLongToBytes(seed)) {}

    /**
     * @return true if this PRNG should create parallel streams; false otherwise.
     */
    protected open fun useParallelStreams(): Boolean {
        return false
    }

    /**
     *
     * Returns true with the given probability, and records that only 1 bit of entropy is being
     * spent.
     *
     *When `probability <= 0`, instantly returns false without recording any
     * entropy spent. Likewise, instantly returns true when `probability >= 1`.
     * @param probability The probability of returning true.
     * @return True with probability equal to the `probability` parameter; false otherwise.
     */
    fun withProbability(
            probability: Double): Boolean {
        if (probability >= 1) {
            return true
        }
        if (probability <= 0) {
            return false
        }
        return if (probability == 0.5) {
            nextBoolean()
        } else withProbabilityInternal(probability)
    }

    /**
     * Called by [.withProbability] to generate a boolean with a specified probability
     * of returning true, after checking that `probability` is strictly between 0 and 1.
     * @param probability The probability (between 0 and 1 exclusive) of returning true.
     * @return True with probability equal to the `probability` parameter; false otherwise.
     */
    open fun withProbabilityInternal(probability: Double): Boolean {
        val result = super.nextDouble() < probability
        // We're only outputting one bit
        debitEntropy(1)
        return result
    }

    /**
     * Chooses a random element from the given array.
     * @param array A non-empty array to choose from.
     * @param <E> The element type of `array`; usually inferred by the compiler.
     * @return An element chosen from `array` at random, with all elements having equal
     * probability.
    </E> */
    fun <E> nextElement(array: Array<E>): E {
        return array[nextInt(array.size)]
    }

    /**
     * Chooses a random element from the given list.
     * @param list A non-empty [List] to choose from.
     * @param <E> The element type of `list`; usually inferred by the compiler.
     * @return An element chosen from `list` at random, with all elements having equal
     * probability.
    </E> */
    fun <E> nextElement(list: List<E>): E {
        return list[nextInt(list.size)]
    }

    /**
     * Chooses a random value of the given enum class.
     * @param enumClass An enum class having at least one value.
     * @param <E> The type of `enumClass`; usually inferred by the compiler.
     * @return A value of `enumClass` chosen at random, with all elements having equal
     * probability.
    </E> */
    fun <E : Enum<E>> nextEnum(enumClass: Class<E>): E {
        return nextElement(enumClass.enumConstants)
    }

    /**
     * Generates the next pseudorandom number. Called by all other random-number-generating methods.
     * Should not debit the entropy count, since that's done by the calling methods according to the
     * amount they actually output (see for example [.withProbability], which uses 53
     * random bits but outputs only one, and thus debits only 1 bit of entropy).
     */
    abstract override fun next(bits: Int): Int

    /**
     * Generates random bytes and places them into a user-supplied byte array. The number of random
     * bytes produced is equal to the length of the byte array. Reimplemented for entropy-counting
     * purposes.
     */
    override fun nextBytes(
            bytes: ByteArray) {
        for (i in bytes.indices) {
            bytes[i] = next(java.lang.Byte.SIZE).toByte()
            debitEntropy(java.lang.Byte.SIZE.toLong())
        }
    }

    override fun nextInt(): Int {
        debitEntropy(Integer.SIZE.toLong())
        return super.nextInt()
    }

    override fun nextInt(bound: Int): Int {
        debitEntropy(entropyOfInt(0, bound).toLong())
        return super.nextInt(bound)
    }

    /**
     * Returns the next pseudorandom, uniformly distributed long value from this random number
     * generator's sequence. Unlike the inherited implementation in [Random.nextLong], ones in
     * BetterRandom generally *can* be expected to return all 2<sup>64</sup> possible values.
     */
    override fun nextLong(): Long {
        val out = nextLongNoEntropyDebit()
        debitEntropy(java.lang.Long.SIZE.toLong())
        return out
    }

    /**
     * Returns a pseudorandom `long` value between zero (inclusive) and the specified bound
     * (exclusive).
     * @param bound the upper bound (exclusive).  Must be positive.
     * @return a pseudorandom `long` value between zero (inclusive) and the bound (exclusive)
     * @throws IllegalArgumentException if `bound` is not positive
     */
    open fun nextLong(bound: Long): Long {
        return nextLong(0, bound)
    }

    /**
     * Returns a pseudorandom `double` value between 0.0 (inclusive) and the specified bound
     * (exclusive).
     * @param bound the upper bound (exclusive).  Must be positive.
     * @return a pseudorandom `double` value between zero (inclusive) and the bound (exclusive)
     * @throws IllegalArgumentException if `bound` is not positive
     */
    @EntryPoint
    open fun nextDouble(bound: Double): Double {
        return nextDouble(0.0, bound)
    }

    /**
     * Returns a pseudorandom `double` value between the specified origin (inclusive) and bound
     * (exclusive).
     * @param origin the least value returned
     * @param bound the upper bound (exclusive)
     * @return a pseudorandom `double` value between the origin (inclusive) and the bound
     * (exclusive)
     * @throws IllegalArgumentException if `origin` is greater than or equal to `bound`
     */
    open fun nextDouble(origin: Double, bound: Double): Double {
        if (bound <= origin) {
            throw IllegalArgumentException(
                    String.format("Bound %f must be greater than origin %f", bound, origin))
        }
        val out = nextDouble() * (bound - origin) + origin
        return if (out >= bound) {
            // correct for rounding
            java.lang.Double.longBitsToDouble(java.lang.Double.doubleToLongBits(bound) - 1)
        } else out
    }

    private fun <T : BaseStream<*, T>> maybeParallel(`in`: T): T {
        return if (useParallelStreams()) `in`.parallel() else `in`
    }

    /**
     *
     * Returns a stream producing an effectively unlimited number of pseudorandom doubles, each
     * conforming to the given origin (inclusive) and bound (exclusive). This implementation uses
     * [.nextDouble] to generate these numbers.
     */
    override fun doubles(randomNumberOrigin: Double,
                         randomNumberBound: Double): DoubleStream {
        return maybeParallel(
                DoubleStream.generate { nextDouble(randomNumberOrigin, randomNumberBound) })
    }

    /**
     *
     * Returns a stream producing an effectively unlimited number of pseudorandom doubles, each
     * between 0.0 (inclusive) and 1.0 (exclusive). This implementation uses [.nextDouble] to
     * generate these numbers.
     */
    override fun doubles(): DoubleStream {
        return maybeParallel(DoubleStream.generate { this.nextDouble() })
    }

    override fun doubles(streamSize: Long): DoubleStream {
        return streamOfSize(streamSize).mapToDouble { ignored -> nextDouble() }
    }

    private fun streamOfSize(streamSize: Long): LongStream {
        return maybeParallel(LongStream.range(0, streamSize).unordered())
    }

    /**
     * Returns a stream producing the given number of pseudorandom doubles, each conforming to the
     * given origin (inclusive) and bound (exclusive). This implementation uses [ ][.nextDouble] to generate these numbers.
     */
    override fun doubles(streamSize: Long, randomNumberOrigin: Double,
                         randomNumberBound: Double): DoubleStream {
        return streamOfSize(streamSize)
                .mapToDouble { ignored -> nextDouble(randomNumberOrigin, randomNumberBound) }
    }

    /**
     *
     * Returns a stream producing an effectively unlimited number of pseudorandom doubles that are
     * normally distributed with mean 0.0 and standard deviation 1.0. This implementation uses [ ][.nextGaussian].
     * @return a stream of normally-distributed random doubles.
     */
    fun gaussians(): DoubleStream {
        return maybeParallel(DoubleStream.generate { this.nextGaussian() })
    }

    /**
     * Returns a stream producing the given number of pseudorandom doubles that are normally
     * distributed with mean 0.0 and standard deviation 1.0. This implementation uses [ ][.nextGaussian].
     * @param streamSize the number of doubles to generate.
     * @return a stream of `streamSize` normally-distributed random doubles.
     */
    fun gaussians(streamSize: Long): DoubleStream {
        return streamOfSize(streamSize).mapToDouble { ignored -> nextGaussian() }
    }

    override fun nextBoolean(): Boolean {
        debitEntropy(1)
        return super.nextBoolean()
    }

    override fun nextFloat(): Float {
        debitEntropy(ENTROPY_OF_FLOAT.toLong())
        return super.nextFloat()
    }

    override fun nextDouble(): Double {
        debitEntropy(ENTROPY_OF_DOUBLE.toLong())
        return nextDoubleNoEntropyDebit()
    }

    /**
     * Returns the next random `double` between 0.0 (inclusive) and 1.0 (exclusive), but does
     * not debit entropy.
     * @return a pseudorandom `double`.
     */
    open fun nextDoubleNoEntropyDebit(): Double {
        lock.lock()
        try {
            return super.nextDouble()
        } finally {
            lock.unlock()
        }
    }

    /**
     * Returns the next pseudorandom, Gaussian ("normally") distributed double value with mean 0.0 and
     * standard deviation 1.0 from this random number generator's sequence. Unlike the one in [ ], this implementation is lockless.
     */
    override fun nextGaussian(): Double {
        // Upper bound. 2 Gaussians are generated from 2 nextDouble calls, which once made are either
        // used or rerolled.
        debitEntropy(ENTROPY_OF_DOUBLE.toLong())
        return internalNextGaussian(DoubleSupplier { this.nextDoubleNoEntropyDebit() })
    }

    /**
     * Core of a reimplementation of [.nextGaussian] whose locking is overridable and doesn't
     * happen when a value is already stored.
     * @param nextDouble shall return a random number between 0 and 1, like [.nextDouble],
     * but shall not debit the entropy count.
     * @return a random number that is normally distributed with mean 0 and standard deviation 1.
     */
    protected fun internalNextGaussian(
            nextDouble: DoubleSupplier): Double {
        // See Knuth, ACP, Section 3.4.1 Algorithm C.
        val firstTryOut = java.lang.Double.longBitsToDouble(nextNextGaussian.getAndSet(NAN_LONG_BITS))
        if (!java.lang.Double.isNaN(firstTryOut)) {
            return firstTryOut
        }
        lockForNextGaussian()
        try {
            // Another output may have become available while we waited for the lock
            val secondTryOut = java.lang.Double.longBitsToDouble(nextNextGaussian.getAndSet(NAN_LONG_BITS))
            if (!java.lang.Double.isNaN(secondTryOut)) {
                return secondTryOut
            }
            var s: Double
            var v1: Double
            var v2: Double
            do {
                v1 = 2 * nextDouble.asDouble - 1 // between -1 and 1
                v2 = 2 * nextDouble.asDouble - 1 // between -1 and 1
                s = v1 * v1 + v2 * v2
            } while (s >= 1 || s == 0.0)
            val multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s)
            nextNextGaussian.set(java.lang.Double.doubleToRawLongBits(v2 * multiplier))
            return v1 * multiplier
        } finally {
            unlockForNextGaussian()
        }
    }

    /** Performs whatever locking is needed by [.nextGaussian].  */
    protected open fun lockForNextGaussian() {
        lock.lock()
    }

    /** Releases the locks acquired by [.lockForNextGaussian].  */
    protected open fun unlockForNextGaussian() {
        lock.unlock()
    }

    override fun ints(streamSize: Long): IntStream {
        return streamOfSize(streamSize).mapToInt { ignored -> nextInt() }
    }

    override fun ints(): IntStream {
        return maybeParallel(IntStream.generate(IntSupplier { this.nextInt() }))
    }

    /**
     * Returns a stream producing the given number of pseudorandom ints, each conforming to the given
     * origin (inclusive) and bound (exclusive). This implementation uses [.nextInt]
     * to generate these numbers.
     */
    override fun ints(streamSize: Long, randomNumberOrigin: Int,
                      randomNumberBound: Int): IntStream {
        return streamOfSize(streamSize)
                .mapToInt { ignored -> nextInt(randomNumberOrigin, randomNumberBound) }
    }

    /**
     * Returns a pseudorandom `int` value between the specified origin (inclusive) and the
     * specified bound (exclusive).
     * @param origin the least value returned
     * @param bound the upper bound (exclusive)
     * @return a pseudorandom `int` value between the origin (inclusive) and the bound
     * (exclusive)
     * @throws IllegalArgumentException if `origin` is greater than or equal to `bound`
     */
    open fun nextInt(origin: Int, bound: Int): Int {
        if (bound <= origin) {
            throw IllegalArgumentException(
                    String.format("Bound %d must be greater than origin %d", bound, origin))
        }
        val range = bound - origin
        if (range >= 0) {
            // range is no more than Integer.MAX_VALUE
            return nextInt(range) + origin
        } else {
            var output: Int
            do {
                output = super.nextInt()
            } while (output < origin || output >= bound)
            debitEntropy(entropyOfInt(origin, bound).toLong())
            return output
        }
    }

    /**
     *
     * Returns a stream producing an effectively unlimited number of pseudorandom ints, each
     * conforming to the given origin (inclusive) and bound (exclusive). This implementation uses
     * [.nextInt] to generate these numbers.
     */
    override fun ints(randomNumberOrigin: Int, randomNumberBound: Int): IntStream {
        return maybeParallel(IntStream.generate { nextInt(randomNumberOrigin, randomNumberBound) })
    }

    override fun longs(streamSize: Long): LongStream {
        return streamOfSize(streamSize).map { ignored -> nextLong() }
    }

    /**
     *
     * {@inheritDoc}
     *
     *If the returned stream is a parallel stream, consuming it in parallel
     * after calling [DoubleStream.limit] may cause extra entropy to be spuriously
     * consumed.
     */
    override fun longs(): LongStream {
        return maybeParallel(LongStream.generate(LongSupplier { this.nextLong() }))
    }

    /**
     *
     * Returns a stream producing the given number of pseudorandom longs, each conforming to the
     * given origin (inclusive) and bound (exclusive). This implementation uses
     * [.nextLong] to generate these numbers.
     */
    override fun longs(streamSize: Long, randomNumberOrigin: Long,
                       randomNumberBound: Long): LongStream {
        return streamOfSize(streamSize).map { ignored -> nextLong(randomNumberOrigin, randomNumberBound) }
    }

    /**
     * Returns a pseudorandom `long` value between the specified origin (inclusive) and the
     * specified bound (exclusive). This implementation is adapted from the reference implementation
     * of [Random.longs] in that method's Javadoc.
     * @param origin the least value returned
     * @param bound the upper bound (exclusive)
     * @return a pseudorandom `long` value between the origin (inclusive) and the bound
     * (exclusive)
     * @throws IllegalArgumentException if `origin` is greater than or equal to `bound`
     */
    open fun nextLong(
            origin: Long, bound: Long): Long {
        if (bound <= origin) {
            throw IllegalArgumentException(
                    String.format("Bound %d must be greater than origin %d", bound, origin))
        }
        lock.lock()
        try {
            var r = nextLongNoEntropyDebit()
            val n = bound - origin
            val m = n - 1
            if (n and m == 0L)
            // power of two
            {
                return (r and m) + origin
            } else if (n > 0L) {  // reject over-represented candidates
                var u = r.ushr(1)            // ensure nonnegative
                while (u + m - (r = u % n) < 0L) {
                    // rejection check
                    u = nextLongNoEntropyDebit().ushr(1)
                } // retry
                r += origin
            } else {              // range not representable as long
                while (r < origin || r >= bound) {
                    r = nextLongNoEntropyDebit()
                }
            }
            return r
        } finally {
            lock.unlock()
            debitEntropy(entropyOfLong(origin, bound).toLong())
        }
    }

    /**
     * Returns the next random `long`, but does not debit entropy.
     * @return a pseudorandom `long` with all possible values equally likely.
     */
    open fun nextLongNoEntropyDebit(): Long {
        lock.lock()
        try {
            return super.nextLong()
        } finally {
            lock.unlock()
        }
    }

    /**
     *
     * Returns a stream producing an effectively unlimited number of pseudorandom longs, each
     * conforming to the given origin (inclusive) and bound (exclusive). This implementation uses
     * [.nextLong] to generate these numbers.
     */
    override fun longs(randomNumberOrigin: Long, randomNumberBound: Long): LongStream {
        return maybeParallel(
                LongStream.generate { nextLong(randomNumberOrigin, randomNumberBound) })
    }

    override fun dump(): String {
        lock.lock()
        try {
            return addSubclassFields(
                    MoreObjects.toStringHelper(this).add("seed", BinaryUtils.convertBytesToHexString(seed_))
                            .add("entropyBits", entropyBits_.get()).add("seedGenerator", seedGenerator))
                    .toString()
        } finally {
            lock.unlock()
        }
    }

    /**
     * Sets the seed of this random number generator using a single long seed, if this implementation
     * supports that. If it is capable of using 64 bits or less of seed data (i.e. if `{
     * #getNewSeedLength()} <= { Long#BYTES}`), then this method shall replace the entire seed as
     * [Random.setSeed] does; otherwise, it shall either combine the input with the
     * existing seed as [java.security.SecureRandom.setSeed] does, or it shall generate a
     * new seed using the [DefaultSeedGenerator].
     *
     */
    @Deprecated("Some implementations are very slow.")
    override fun setSeed(seed: Long) {
        val seedBytes = BinaryUtils.convertLongToBytes(seed)
        if (superConstructorFinished) {
            setSeed(seedBytes)
        } else {
            setSeedInternal(seedBytes)
        }
    }

    /**
     * {@inheritDoc}
     *
     *Most subclasses should override [.setSeedInternal] instead of
     * this method, so that they will deserialize properly.
     */
    override fun setSeed(seed: ByteArray) {
        lock.lock()
        try {
            setSeedInternal(seed)
        } finally {
            lock.unlock()
        }
    }

    /**
     * Adds the fields that were not inherited from BaseRandom to the given [ ] for dumping.
     * @param original a [ToStringHelper] object.
     * @return `original` with the fields not inherited from BaseRandom written to it.
     */
    protected abstract fun addSubclassFields(original: ToStringHelper): ToStringHelper

    /**
     * Registers this PRNG with the [RandomSeederThread] for the corresponding [ ], to schedule reseeding when we run out of entropy. Unregisters this PRNG with
     * the previous [RandomSeederThread] if it had a different one.
     * @param seedGenerator a [SeedGenerator] whose [RandomSeederThread] will be used
     * to reseed this PRNG, or null to stop using one.
     */
    open fun setSeedGenerator(seedGenerator: SeedGenerator?) {
        val oldSeedGenerator = this.seedGenerator.getAndSet(seedGenerator)
        if (seedGenerator !== oldSeedGenerator) {
            if (oldSeedGenerator != null) {
                RandomSeederThread.remove(oldSeedGenerator, this)
            }
            if (seedGenerator != null) {
                RandomSeederThread.add(seedGenerator, this)
            }
        }
    }

    /**
     * Returns the current seed generator for this PRNG.
     * @return the current seed generator, or null if there is none
     */
    fun getSeedGenerator(): SeedGenerator? {
        return seedGenerator.get()
    }

    override fun preferSeedWithLong(): Boolean {
        return newSeedLength <= java.lang.Long.BYTES
    }

    /**
     * Sets the seed, and should be overridden to set other state that derives from the seed. Called
     * by [.setSeed], constructors, [.readObject] and [ ][.fallbackSetSeed]. When called after initialization, the [.lock] is always held.
     * @param seed The new seed.
     */
    protected open fun setSeedInternal(seed: ByteArray?) {
        if (this.seed_ == null || this.seed_!!.size != seed!!.size) {
            this.seed_ = seed!!.clone()
        } else {
            System.arraycopy(seed, 0, this.seed_!!, 0, seed.size)
        }
        nextNextGaussian.set(NAN_LONG_BITS) // Invalidate Gaussian that was generated from old seed
        creditEntropyForNewSeed(seed.size)
    }

    /**
     * Updates the entropy count to reflect a reseeding. Sets it to the seed length or the internal
     * state size, whichever is shorter, but never less than the existing entropy count.
     * @param seedLength the length of the new seed in bytes
     */
    open fun creditEntropyForNewSeed(seedLength: Int) {
        entropyBits_.updateAndGet { oldCount -> Math.max(oldCount, Math.min(seedLength, newSeedLength) * 8L) }
    }

    /**
     * Called in constructor and readObject to initialize transient fields.
     */
    protected open fun initTransientFields() {
        superConstructorFinished = true
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(`in`: ObjectInputStream) {
        `in`.defaultReadObject()
        initTransientFields()
        setSeedInternal(seed_)
        val currentSeedGenerator = getSeedGenerator()
        if (currentSeedGenerator != null) {
            RandomSeederThread.add(currentSeedGenerator, this)
        }
    }

    /**
     * Record that entropy has been spent, and schedule a reseeding if this PRNG has now spent as much
     * as it's been seeded with.
     * @param bits The number of bits of entropy spent.
     */
    open fun debitEntropy(bits: Long) {
        if (entropyBits_.addAndGet(-bits) <= 0) {
            asyncReseedIfPossible()
        }
    }

    private fun asyncReseedIfPossible() {
        val currentSeedGenerator = getSeedGenerator()
        if (currentSeedGenerator != null) {
            RandomSeederThread.asyncReseed(currentSeedGenerator, this)
        }
    }

    /**
     * Used to deserialize a subclass instance that wasn't a subclass instance when it was serialized.
     * Since that means we can't deserialize our seed, we generate a new one with the [ ].
     * @throws InvalidObjectException if the [DefaultSeedGenerator] fails.
     */
    @Throws(InvalidObjectException::class)
    private fun readObjectNoData() {
        LOG.warn("BaseRandom.readObjectNoData() invoked; using DefaultSeedGenerator")
        try {
            fallbackSetSeed()
        } catch (e: RuntimeException) {
            throw InvalidObjectException(
                    "Failed to deserialize or generate a seed").initCause(e.cause) as InvalidObjectException
        }

        initTransientFields()
        setSeedInternal(seed_)
    }

    /**
     * Generates a seed using the default seed generator if there isn't one already. For use in
     * handling a [.setSeed] call from the super constructor [Random.Random] in
     * subclasses that can't actually use an 8-byte seed. Also used in [.readObjectNoData].
     * Does not acquire the lock, because it's normally called from an initializer.
     */
    protected fun fallbackSetSeed() {
        if (seed_ == null) {
            seed_ = DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(newSeedLength)
        }
    }

    protected fun fallbackSetSeedIfInitialized() {
        if (!superConstructorFinished) {
            return
        }
        lock.lock()
        try {
            setSeedInternal(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(newSeedLength))
        } finally {
            lock.unlock()
        }
    }

    companion object {

        /** The number of pseudorandom bits in [.nextFloat].  */
        val ENTROPY_OF_FLOAT = 24

        /** The number of pseudorandom bits in [.nextDouble].  */
        val ENTROPY_OF_DOUBLE = 53

        private val NAN_LONG_BITS = java.lang.Double.doubleToLongBits(java.lang.Double.NaN)
        private val LOG = LoggerFactory.getLogger(BaseRandom::class.java)
        private val serialVersionUID = -1556392727255964947L

        /**
         * Calculates the entropy in bits, rounded up, of a random `int` between `origin`
         * (inclusive) and `bound` (exclusive).
         * @param origin the minimum, inclusive.
         * @param bound the maximum, exclusive.
         * @return the entropy.
         */
        fun entropyOfInt(origin: Int, bound: Int): Int {
            return Integer.SIZE - Integer.numberOfLeadingZeros(bound - origin - 1)
        }

        /**
         * Calculates the entropy in bits, rounded up, of a random `long` between `origin`
         * (inclusive) and `bound` (exclusive).
         * @param origin the minimum, inclusive.
         * @param bound the maximum, exclusive.
         * @return the entropy.
         */
        fun entropyOfLong(origin: Long, bound: Long): Int {
            return java.lang.Long.SIZE - java.lang.Long.numberOfLeadingZeros(bound - origin - 1)
        }
    }
}
