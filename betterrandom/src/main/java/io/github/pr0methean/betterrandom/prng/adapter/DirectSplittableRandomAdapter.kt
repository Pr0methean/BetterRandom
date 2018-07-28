package io.github.pr0methean.betterrandom.prng.adapter

import com.google.common.base.MoreObjects.ToStringHelper
import io.github.pr0methean.betterrandom.util.BinaryUtils
import io.github.pr0methean.betterrandom.util.EntryPoint
import java.io.IOException
import java.io.ObjectInputStream
import java.util.SplittableRandom

/**
 * Abstract subclass of [BaseSplittableRandomAdapter] where [.setSeed] and [ ][.setSeed] replace the [SplittableRandom] that's used in the context in which they
 * are called. See [ReseedingSplittableRandomAdapter] for an example of when it does
 * *not* make sense to extend this class.
 * @author Chris Hennick
 */
abstract class DirectSplittableRandomAdapter : BaseSplittableRandomAdapter {

    /**
     * The master [SplittableRandom] that will either be delegated to directly (see [ ] or be split using [SplittableRandom.split] (see
     * [SplittableRandomAdapter]) and have the splits delegated to.
     */
    @Volatile
    @Transient
    protected var underlying: SplittableRandom // SplittableRandom isn't Serializable

    /**
     * Wraps a [SplittableRandom] with the specified seed.
     * @param seed 8 bytes of seed data used to initialise the RNG.
     */
    protected constructor(seed: ByteArray) : super(seed) {}

    /**
     * Wraps a [SplittableRandom] with the specified seed.
     * @param seed the seed.
     */
    @EntryPoint protected constructor(seed: Long) : super(seed) {
        setSeed(seed)
    }

    override fun addSubclassFields(original: ToStringHelper): ToStringHelper {
        return original.add("underlying", underlying)
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(`in`: ObjectInputStream) {
        `in`.defaultReadObject()
        setSeedInternal(seed)
    }

    /**
     * Replaces [.underlying] with a new [SplittableRandom] that uses the given seed.
     */
    override fun setSeedInternal(seed: ByteArray?) {
        if (seed!!.size != java.lang.Long.BYTES) {
            throw IllegalArgumentException("DirectSplittableRandomAdapter requires an 8-byte seed")
        }
        super.setSeedInternal(seed)
        underlying = SplittableRandom(BinaryUtils.convertBytesToLong(seed))
    }

    /**
     * Replaces [.underlying] with a new [SplittableRandom] that uses the given seed.
     */
    override fun setSeed(seed: Long) {
        if (superConstructorFinished) {
            super.setSeedInternal(BinaryUtils.convertLongToBytes(seed))
        }
        underlying = SplittableRandom(seed)
    }

    companion object {

        private val serialVersionUID = 4273652147052638879L
    }
}
