package io.github.pr0methean.betterrandom.util

import org.testng.Assert.assertTrue

import com.google.common.collect.ImmutableMap
import io.github.pr0methean.betterrandom.MockException
import io.github.pr0methean.betterrandom.TestUtils
import java.io.Serializable
import java.lang.Thread.UncaughtExceptionHandler
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.testng.annotations.BeforeTest
import org.testng.annotations.Test

class LooperThreadTest {

    @Test
    @Throws(IllegalAccessException::class, InstantiationException::class, InvocationTargetException::class)
    fun testConstructors() {
        TestUtils.testConstructors(LooperThread::class.java, false, ImmutableMap
                .of(ThreadGroup::class.java, ThreadGroup("Test ThreadGroup"), Runnable::class.java, TARGET,
                        String::class.java, "Test LooperThread", Long::class.javaPrimitiveType!!, STACK_SIZE)) { thread ->
            thread.start()
            try {
                assertTrue(thread.awaitIteration(1, TimeUnit.SECONDS))
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
        }
    }

    @BeforeTest
    fun setUp() {
        shouldThrow.set(false)
        exceptionHandlerRun.set(false)
    }

    @Test(expectedExceptions = arrayOf(UnsupportedOperationException::class))
    fun testMustOverrideIterate() {
        LooperThread().run()
    }

    @Test
    @Throws(InterruptedException::class)
    fun testDefaultUncaughtExceptionHandler() {
        val defaultHandlerCalled = AtomicBoolean(false)
        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
        try {
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable -> defaultHandlerCalled.set(true) }
            val failingThread = FailingLooperThread()
            failingThread.start()
            failingThread.join()
            Thread.sleep(1000)
            assertTrue(defaultHandlerCalled.get())
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(oldHandler)
        }
    }

    @Test
    @Throws(InterruptedException::class)
    fun testAwaitIteration() {
        val sleepingThread = SleepingLooperThread()
        sleepingThread.start()
        try {
            assertTrue(sleepingThread.awaitIteration(3, TimeUnit.SECONDS))
            // Now do so again, to ensure the thread still runs after returning
            assertTrue(sleepingThread.awaitIteration(3, TimeUnit.SECONDS))
        } finally {
            sleepingThread.interrupt()
        }
    }

    private class FailingLooperThread : LooperThread("FailingLooperThread") {

        public override fun iterate(): Boolean {
            throw MockException()
        }
    }

    private class SleepingLooperThread : LooperThread("SleepingLooperThread") {

        @Throws(InterruptedException::class)
        public override fun iterate(): Boolean {
            Thread.sleep(100)
            return finishedIterations.get() < 50
        }
    }

    companion object {

        private val STACK_SIZE: Long = 1234567
        private val shouldThrow = AtomicBoolean(false)
        private val exceptionHandlerRun = AtomicBoolean(false)
        private val TARGET = {
            if (shouldThrow.get()) {
                throw MockException()
            } else {
                try {
                    Thread.sleep(1)
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                }

            }
        } as Serializable
    }
}
