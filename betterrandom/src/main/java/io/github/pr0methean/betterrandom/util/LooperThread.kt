package io.github.pr0methean.betterrandom.util

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * Thread that loops a given task until interrupted (or until JVM shutdown, if it [ ][.isDaemon]), with the iterations being transactional.
 */
open class LooperThread : Thread {

    protected val finishedIterations = AtomicLong(0)
    /**
     * The thread holds this lock whenever it is being serialized or cloned or is running [ ][.iterate] called by [.run].
     */
    protected val lock: Lock = ReentrantLock(true)
    protected val endOfIteration = lock.newCondition()
    /**
     * The [Runnable] that was passed into this thread's constructor, if any.
     */
    protected var target: Runnable? = null

    /**
     * Constructs a LooperThread with the given name and target. `target` should only be null if
     * called from a subclass that overrides [.iterate].
     * @param target If not null, the target this thread will run in [.iterate].
     * @param name the thread name
     */
    constructor(target: Runnable?, name: String) : super(target, name) {
        this.target = target
    }

    /**
     * Constructs a LooperThread that belongs to the given [ThreadGroup] and has the given
     * target. `target` should only be null if called from a subclass that overrides [ ][.iterate].
     * @param group The ThreadGroup this thread will belong to.
     * @param target If not null, the target this thread will run in [.iterate].
     */
    constructor(group: ThreadGroup, target: Runnable?) : super(group, target) {
        this.target = target
    }

    /**
     *
     * Constructs a LooperThread with the given name and target, belonging to the given [ ] and having the given preferred stack size. `target` should only be null if
     * called from a subclass that overrides [.iterate].
     *
     * See [Thread.Thread] for caveats about
     * specifying the stack size.
     * @param group The ThreadGroup this thread will belong to.
     * @param target If not null, the target this thread will run in [.iterate].
     * @param name the thread name
     * @param stackSize the desired stack size for the new thread, or zero to indicate that this
     * parameter is to be ignored.
     */
    constructor(group: ThreadGroup, target: Runnable?, name: String,
                stackSize: Long) : super(group, target, name, stackSize) {
        this.target = target
    }

    /**
     * Constructs a LooperThread with the given name and belonging to the given [ThreadGroup].
     * Protected because it does not set a target, and thus should only be used in subclasses that
     * override [.iterate].
     * @param group The ThreadGroup this thread will belong to.
     * @param name the thread name
     */
    protected constructor(group: ThreadGroup, name: String) : super(group, name) {}

    /**
     * Constructs a LooperThread with the given target. `target` should only be null if called
     * from a subclass that overrides [.iterate].
     * @param target If not null, the target this thread will run in [.iterate].
     */
    constructor(target: Runnable?) : super(target) {
        this.target = target
    }

    /**
     * Constructs a LooperThread with the given name and target, belonging to the given [ ]. `target` should only be null if called from a subclass that overrides
     * [.iterate].
     * @param group The ThreadGroup this thread will belong to.
     * @param target If not null, the target this thread will run in [.iterate].
     * @param name the thread name
     */
    constructor(group: ThreadGroup, target: Runnable?, name: String) : super(group, target, name) {
        this.target = target
    }

    /**
     * Constructs a LooperThread with the given name. Protected because it does not set a target, and
     * thus should only be used in subclasses that override [.iterate].
     * @param name the thread name
     */
    protected constructor(name: String) : super(name) {}

    /**
     * Constructs a LooperThread with all properties as defaults. Protected because it does not set a
     * target, and thus should only be used in subclasses that override [.iterate].
     */
    constructor() {}

    /**
     * The task that will be iterated until it returns false. Cannot be abstract for serialization
     * reasons, but must be overridden in subclasses if they are instantiated without a target [ ].
     * @return true if this thread should iterate again.
     * @throws InterruptedException if interrupted in mid-execution.
     * @throws UnsupportedOperationException if this method has not been overridden and [     ][.target] was not set to non-null during construction.
     */
    @Throws(InterruptedException::class)
    protected open fun iterate(): Boolean {
        if (target == null) {
            throw UnsupportedOperationException("This method should be overridden, or else this " + "thread should have been created with a Serializable target!")
        } else {
            target!!.run()
            return true
        }
    }

    /**
     * Runs [.iterate] until either it returns false or this thread is interrupted.
     */
    override fun run() {
        while (true) {
            try {
                lock.lockInterruptibly()
                try {
                    val shouldContinue = iterate()
                    finishedIterations.getAndIncrement()
                    if (!shouldContinue) {
                        break
                    }
                } finally {
                    endOfIteration.signalAll()
                    lock.unlock()
                }
            } catch (ignored: InterruptedException) {
                interrupt()
                break
            }

        }
    }

    /**
     * Wait for the next iteration to finish, with a timeout. May wait longer in the event of a
     * spurious wakeup.
     * @param time the maximum time to wait
     * @param unit the time unit of the `time` argument
     * @return `false`  the waiting time detectably elapsed before an iteration finished, else
     * `true`
     * @throws InterruptedException if thrown by [Condition.await]
     */
    @Throws(InterruptedException::class)
    fun awaitIteration(time: Long, unit: TimeUnit): Boolean {
        val previousFinishedIterations = finishedIterations.get()
        lock.lock()
        try {
            while (!isInterrupted && state != Thread.State.TERMINATED && finishedIterations.get() == previousFinishedIterations) {
                endOfIteration.await(time, unit)
            }
            return finishedIterations.get() != previousFinishedIterations
        } finally {
            lock.unlock()
        }
    }
}
