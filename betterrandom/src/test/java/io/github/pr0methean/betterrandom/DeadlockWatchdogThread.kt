package io.github.pr0methean.betterrandom

import io.github.pr0methean.betterrandom.util.LooperThread
import java.lang.management.ManagementFactory
import java.lang.management.ThreadInfo
import java.lang.management.ThreadMXBean
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// intermittently needed for debugging
class DeadlockWatchdogThread private constructor() : LooperThread("DeadlockWatchdogThread") {

    private class StackTraceHolder(name: String, stackTrace: Array<StackTraceElement>) : Throwable(name, null, false, true) {
        init {
            setStackTrace(stackTrace)
        }

        @Synchronized
        override fun fillInStackTrace(): Throwable {
            // No-op: we only use the stack trace that's in our constructor parameter
            return this
        }
    }

    @Throws(InterruptedException::class)
    public override fun iterate(): Boolean {
        Thread.sleep(60000)
        var deadlockFound = false
        var threadsOfInterest: LongArray? = THREAD_MX_BEAN.findDeadlockedThreads()
        if (threadsOfInterest != null && threadsOfInterest.size > 0) {
            LOG.error("DEADLOCKED THREADS FOUND")
            deadlockFound = true
        } else {
            threadsOfInterest = THREAD_MX_BEAN.allThreadIds
            if (threadsOfInterest!!.size <= 0) {
                LOG.error("ThreadMxBean didn't return any thread IDs")
                return false
            }
        }
        for (id in threadsOfInterest) {
            val threadInfo = THREAD_MX_BEAN.getThreadInfo(id, MAX_STACK_DEPTH)
            val stackTrace = threadInfo.stackTrace
            val t = StackTraceHolder(threadInfo.threadName, stackTrace)
            if (deadlockFound) {
                LOG.error("A deadlocked thread:", t)
            } else {
                LOG.info("A running thread:", t)
            }
        }
        if (deadlockFound) {
            // Fail fast if current context allows
            System.exit(DEADLOCK_STATUS)
        }
        return !deadlockFound // Terminate when a deadlock is found
    }

    companion object {

        private val THREAD_MX_BEAN = ManagementFactory.getThreadMXBean()
        private val LOG = LoggerFactory.getLogger(DeadlockWatchdogThread::class.java)
        private val MAX_STACK_DEPTH = 20
        private val DEADLOCK_STATUS = -0x2152ef34
        private var INSTANCE = DeadlockWatchdogThread()

        fun ensureStarted() {
            synchronized(DeadlockWatchdogThread::class.java) {
                if (INSTANCE.state == Thread.State.TERMINATED) {
                    INSTANCE = DeadlockWatchdogThread()
                }
                if (INSTANCE.state == Thread.State.NEW) {
                    INSTANCE.isDaemon = true
                    INSTANCE.priority = Thread.MAX_PRIORITY
                    INSTANCE.start()
                }
            }
        }

        fun stopInstance() {
            synchronized(DeadlockWatchdogThread::class.java) {
                INSTANCE.interrupt()
                INSTANCE = DeadlockWatchdogThread()
            }
        }
    }
}
