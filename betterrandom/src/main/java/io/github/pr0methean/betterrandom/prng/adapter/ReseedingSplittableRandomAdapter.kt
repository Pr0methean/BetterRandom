package io.github.pr0methean.betterrandom.prng.adapter

import com.google.common.base.MoreObjects.ToStringHelper
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator
import io.github.pr0methean.betterrandom.seed.RandomSeederThread
import io.github.pr0methean.betterrandom.seed.SeedException
import io.github.pr0methean.betterrandom.seed.SeedGenerator
import java.io.IOException
import java.io.ObjectInputStream
import java.util.Collections
import java.util.SplittableRandom
import java.util.WeakHashMap

/**
 * Like [SplittableRandomAdapter], but uses a [RandomSeederThread] to replace each
 * thread's [SplittableRandom] with a reseeded one as frequently as possible, but not more
 * frequently than it is being used.
 * @author Chris Hennick
 */
class ReseedingSplittableRandomAdapter
/**
 * Single instance per SeedGenerator.
 * @param seedGenerator The seed generator this adapter will use.
 */
@Throws(SeedException::class)
private constructor(private val seedGenerator: SeedGenerator) : BaseSplittableRandomAdapter(seedGenerator.generateSeed(java.lang.Long.BYTES)) {
    @Transient
    private var threadLocal: ThreadLocal<SingleThreadSplittableRandomAdapter>? = null

    override val entropyBits: Long
        get() = threadLocal!!.get().entropyBits

    override val seed: ByteArray
        get() = threadLocal!!.get().seed

    protected override val splittableRandom: SplittableRandom
        get() {
            val adapterForThread = threadLocal!!.get()
            RandomSeederThread.add(seedGenerator, adapterForThread)
            return adapterForThread.splittableRandom
        }

    init {
        initSubclassTransientFields()
    }

    override fun setSeedGenerator(seedGenerator: SeedGenerator?) {
        throw UnsupportedOperationException(
                "ReseedingSplittableRandomAdapter's binding to RandomSeederThread is immutable")
    }

    override fun useParallelStreams(): Boolean {
        return true
    }

    override fun addSubclassFields(original: ToStringHelper): ToStringHelper {
        return original.add("threadLocal", threadLocal!!)
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(`in`: ObjectInputStream) {
        `in`.defaultReadObject()
        initSubclassTransientFields()
    }

    private fun readResolve(): ReseedingSplittableRandomAdapter {
        return getInstance(seedGenerator)
    }

    private fun initSubclassTransientFields() {
        if (threadLocal == null) {
            threadLocal = ThreadLocal.withInitial { SingleThreadSplittableRandomAdapter(seedGenerator) }
        }
    }

    override fun debitEntropy(bits: Long) {
        // Necessary because our inherited next* methods read straight through to the SplittableRandom.
        threadLocal!!.get().debitEntropy(bits)
    }

    override fun equals(o: Any?): Boolean {
        return this === o || o is ReseedingSplittableRandomAdapter && seedGenerator == o.seedGenerator
    }

    override fun setSeedInternal(seed: ByteArray?) {
        this.seed_ = seed!!.clone()
    }

    override fun hashCode(): Int {
        return seedGenerator.hashCode() + 1
    }

    override fun toString(): String {
        return "ReseedingSplittableRandomAdapter using $seedGenerator"
    }

    companion object {

        private val serialVersionUID = 6301096404034224037L
        private val INSTANCES = Collections.synchronizedMap(WeakHashMap<SeedGenerator, ReseedingSplittableRandomAdapter>(1))

        /**
         * Returns the instance backed by the [DefaultSeedGenerator].
         * @return The instance backed by the [DefaultSeedGenerator].
         * @throws SeedException if the [DefaultSeedGenerator] throws one while generating the
         * initial seed.
         */
        val defaultInstance: ReseedingSplittableRandomAdapter
            @Throws(SeedException::class)
            get() = getInstance(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR)

        /**
         * Returns the instance backed by the given [SeedGenerator].
         * @param seedGenerator The seed generator the returned adapter is to use.
         * @return the ReseedingSplittableRandomAdapter backed by `seedGenerator`.
         * @throws SeedException if `seedGenerator` throws one while generating the initial
         * seed.
         */
        @Throws(SeedException::class)
        fun getInstance(seedGenerator: SeedGenerator): ReseedingSplittableRandomAdapter {
            synchronized(INSTANCES) {
                return (INSTANCES as java.util.Map<SeedGenerator, ReseedingSplittableRandomAdapter>).computeIfAbsent(seedGenerator, Function<SeedGenerator, ReseedingSplittableRandomAdapter> { ReseedingSplittableRandomAdapter(it) })
            }
        }
    }
}
