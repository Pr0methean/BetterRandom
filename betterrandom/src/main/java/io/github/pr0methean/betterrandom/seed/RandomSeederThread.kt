package io.github.pr0methean.betterrandom.seed

import com.google.common.cache.CacheBuilder
import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom
import io.github.pr0methean.betterrandom.EntropyCountingRandom
import io.github.pr0methean.betterrandom.util.LooperThread
import java.nio.ByteBuffer
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashSet
import java.util.LinkedList
import java.util.Random
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Condition
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Thread that loops over [Random] instances and reseeds them. No [ ] will be reseeded when it's already had more input than output.
 * @author Chris Hennick
 */
class RandomSeederThread
/**
 * Private constructor because only one instance per seed source.
 */
private constructor(private val seedGenerator: SeedGenerator) : LooperThread() {
    private val waitWhileEmpty = lock.newCondition()
    private val waitForEntropyDrain = lock.newCondition()
    private val prngs = Collections.newSetFromMap(CacheBuilder.newBuilder()
            .weakKeys()
            .initialCapacity(1)
            .build<Random, Boolean>()
            .asMap())
    private val longSeedArray = ByteArray(8)
    private val longSeedBuffer = ByteBuffer.wrap(longSeedArray)
    private val prngsThisIteration = HashSet<Random>(1)
    private val seedArrays = WeakHashMap<ByteArrayReseedableRandom, ByteArray>(1)

    /**
     * Returns true if no [Random] instances are registered with this RandomSeederThread.
     * @return true if no [Random] instances are registered with this RandomSeederThread.
     */
    private val isEmpty: Boolean
        get() {
            lock.lock()
            try {
                return prngs.isEmpty()
            } finally {
                lock.unlock()
            }
        }

    /**
     * Asynchronously triggers reseeding of the given [EntropyCountingRandom] if it is
     * associated with a live RandomSeederThread.
     * @param random a [Random] object.
     * @return Whether or not the reseed was successfully scheduled.
     */
    private fun asyncReseed(random: Random): Boolean {
        if (!isAlive || !prngs.contains(random)) {
            return false
        }
        if (random is EntropyCountingRandom) {
            // Reseed of non-entropy-counting Random happens every iteration anyway
            WAKER_UPPER.submit {
                lock.lock()
                try {
                    waitForEntropyDrain.signalAll()
                } finally {
                    lock.unlock()
                }
            }
        }
        return true
    }

    @Throws(InterruptedException::class)
    override fun iterate(): Boolean {
        while (true) {
            prngsThisIteration.addAll(prngs)
            if (prngsThisIteration.isEmpty()) {
                waitWhileEmpty.await()
            } else {
                break
            }
        }
        val iterator = prngsThisIteration.iterator()
        var entropyConsumed = false
        while (iterator.hasNext()) {
            val random = iterator.next()
            iterator.remove()
            if (random is EntropyCountingRandom && (random as EntropyCountingRandom).entropyBits > 0) {
                continue
            } else {
                entropyConsumed = true
            }
            try {
                if (random is ByteArrayReseedableRandom && !(random as ByteArrayReseedableRandom)
                                .preferSeedWithLong()) {
                    val reseedable = random as ByteArrayReseedableRandom
                    val seedArray = (seedArrays as java.util.Map<ByteArrayReseedableRandom, ByteArray>)
                            .computeIfAbsent(reseedable) { random_ -> ByteArray(random_.newSeedLength) }
                    seedGenerator.generateSeed(seedArray)
                    reseedable.setSeed(seedArray)
                } else {
                    seedGenerator.generateSeed(longSeedArray)
                    random.setSeed(longSeedBuffer.getLong(0))
                }
            } catch (t: Throwable) {
                // Must unlock before interrupt; otherwise we somehow get a deadlock
                lock.unlock()
                LOG.error("Error during reseeding; disabling the RandomSeederThread for $seedGenerator",
                        t)
                interrupt()
                // Must lock again before returning, so we can notify conditions
                lock.lock()
                return false
            }

        }
        if (!entropyConsumed) {
            waitForEntropyDrain.await(POLL_INTERVAL, TimeUnit.SECONDS)
        }
        return true
    }

    override fun interrupt() {
        // Ensure dying instance is unregistered
        (INSTANCES as java.util.Map<SeedGenerator, RandomSeederThread>).remove(seedGenerator, this)
        super.interrupt()
        lock.lock()
        try {
            prngs.clear()
            prngsThisIteration.clear()
            seedArrays.clear()
        } finally {
            lock.unlock()
        }
    }

    /**
     * Add one or more [Random] instances. The caller must not hold locks on any of these
     * instances that are also acquired during [Random.setSeed] or [ ][ByteArrayReseedableRandom.setSeed], as one of those methods may be called immediately
     * and this would cause a circular deadlock.
     * @param randoms One or more [Random] instances to be reseeded.
     */
    private fun add(vararg randoms: Random) {
        lock.lock()
        try {
            if (state == Thread.State.TERMINATED || isInterrupted) {
                throw IllegalStateException("Already shut down")
            }
            Collections.addAll(prngs, *randoms)
            waitForEntropyDrain.signalAll()
            waitWhileEmpty.signalAll()
        } finally {
            lock.unlock()
        }
    }

    /**
     * Remove one or more [Random] instances. If this is called while [.getState] ==
     * [State.RUNNABLE], they may still be reseeded once more.
     * @param randoms the [Random] instances to remove.
     */
    private fun remove(vararg randoms: Random) {
        prngs.removeAll(Arrays.asList(*randoms))
    }

    /**
     * Shut down this thread if no [Random] instances are registered with it.
     */
    private fun stopIfEmpty() {
        lock.lock()
        try {
            if (isEmpty) {
                LOG.info("Stopping empty RandomSeederThread for {}", seedGenerator)
                interrupt()
            }
        } finally {
            lock.unlock()
        }
    }

    companion object {

        private val WAKER_UPPER = Executors.newSingleThreadExecutor()
        private val LOG = LoggerFactory.getLogger(RandomSeederThread::class.java)
        private val INSTANCES = ConcurrentHashMap<SeedGenerator, RandomSeederThread>(1)
        private val POLL_INTERVAL: Long = 60
        private val defaultPriority = AtomicInteger(Thread.NORM_PRIORITY)

        /**
         * Obtain the instance for the given [SeedGenerator], creating and starting it if it doesn't
         * exist.
         * @param seedGenerator the [SeedGenerator] to use to seed PRNGs registered with this
         * RandomSeederThread.
         * @return a RandomSeederThread that is running and is backed by `seedGenerator`.
         */
        private fun getInstance(seedGenerator: SeedGenerator): RandomSeederThread {
            return (INSTANCES as java.util.Map<SeedGenerator, RandomSeederThread>).computeIfAbsent(seedGenerator) { seedGen ->
                LOG.info("Creating a RandomSeederThread for {}", seedGen)
                val thread = RandomSeederThread(seedGen)
                thread.name = "RandomSeederThread for $seedGen"
                thread.isDaemon = true
                thread.priority = defaultPriority.get()
                thread.start()
                thread
            }
        }

        /**
         * Returns whether a RandomSeederThread using the given [SeedGenerator] is running or not.
         * @param seedGenerator a [SeedGenerator] to find an instance for.
         * @return true if a RandomSeederThread using the given [SeedGenerator] is running; false
         * otherwise.
         */
        fun hasInstance(seedGenerator: SeedGenerator): Boolean {
            return INSTANCES.containsKey(seedGenerator)
        }

        /**
         * Shut down all instances with which no [Random] instances are registered.
         */
        fun stopAllEmpty() {
            val toStop = LinkedList(INSTANCES.values)
            for (instance in toStop) {
                instance.stopIfEmpty()
            }
        }

        /**
         * Asynchronously triggers reseeding of the given [EntropyCountingRandom] if it is
         * associated with a live RandomSeederThread corresponding to the given [SeedGenerator].
         * @param seedGenerator the [SeedGenerator] that should reseed `random`
         * @param random a [Random] to be reseeded
         * @return Whether or not the reseed was successfully scheduled.
         */
        fun asyncReseed(seedGenerator: SeedGenerator, random: Random): Boolean {
            val thread = getInstance(seedGenerator)
            return thread != null && thread.asyncReseed(random)
        }

        fun isEmpty(seedGenerator: SeedGenerator): Boolean {
            val thread = getInstance(seedGenerator)
            return thread == null || thread.isEmpty
        }

        /**
         * Add one or more [Random] instances to the thread for the given [SeedGenerator].
         * @param seedGenerator The [SeedGenerator] that will reseed the `randoms`
         * @param randoms One or more [Random] instances to be reseeded
         */
        fun add(seedGenerator: SeedGenerator, vararg randoms: Random) {
            var notSucceeded = true
            do {
                try {
                    getInstance(seedGenerator).add(*randoms)
                    notSucceeded = false
                } catch (ignored: IllegalStateException) {
                    // Get the new instance and try again.
                }

            } while (notSucceeded)
        }

        /**
         * Remove one or more [Random] instances from the thread for the given [SeedGenerator]
         * if such a thread exists and contains them.
         * @param seedGenerator The [SeedGenerator] that will reseed the `randoms`
         * @param randoms One or more [Random] instances to be reseeded
         */
        fun remove(seedGenerator: SeedGenerator, vararg randoms: Random) {
            val thread = INSTANCES[seedGenerator]
            thread?.remove(*randoms)
        }

        /**
         * Sets the default priority for new random-seeder threads.
         * @param priority the thread priority
         * @see Thread.setPriority
         */
        fun setDefaultPriority(priority: Int) {
            defaultPriority.set(priority)
        }

        /**
         * Sets the priority of a random-seeder thread, starting it if it's not already running.
         * @param seedGenerator the [SeedGenerator] of the thread whose priority should change
         * @param priority the thread priority
         * @see Thread.setPriority
         */
        fun setPriority(seedGenerator: SeedGenerator, priority: Int) {
            getInstance(seedGenerator).priority = priority
        }

        fun stopIfEmpty(seedGenerator: SeedGenerator) {
            val thread = INSTANCES[seedGenerator]
            thread?.stopIfEmpty()
        }
    }
}
