package io.github.pr0methean.betterrandom.seed

import io.github.pr0methean.betterrandom.seed.RandomSeederThread.stopAllEmpty
import org.testng.Assert.assertEquals
import org.testng.Assert.assertFalse
import org.testng.Assert.assertTrue

import java.util.Arrays
import java.util.Locale
import java.util.Random
import org.testng.annotations.Test

class RandomSeederThreadTest {

    @Test(timeOut = 25000)
    @Throws(Exception::class)
    fun testAddRemoveAndIsEmpty() {
        val prng = Random(TEST_SEED)
        val bytesWithOldSeed = ByteArray(TEST_OUTPUT_SIZE)
        prng.nextBytes(bytesWithOldSeed)
        prng.setSeed(TEST_SEED) // Rewind
        val seedGenerator = FakeSeedGenerator("testAddRemoveAndIsEmpty")
        try {
            assertTrue(RandomSeederThread.isEmpty(seedGenerator))
            RandomSeederThread.add(seedGenerator, prng)
            assertFalse(RandomSeederThread.isEmpty(seedGenerator))
            if (ON_LINUX) {
                // FIXME: sleep gets interrupted on Travis-CI OSX & on Appveyor
                Thread.sleep(250)
                assertFalse(RandomSeederThread.isEmpty(seedGenerator))
            }
            RandomSeederThread.remove(seedGenerator, prng)
            assertTrue(RandomSeederThread.isEmpty(seedGenerator))
            val bytesWithNewSeed = ByteArray(TEST_OUTPUT_SIZE)
            prng.nextBytes(bytesWithNewSeed)
            if (ON_LINUX) {
                // FIXME: Fails without the Thread.sleep call
                assertFalse(Arrays.equals(bytesWithOldSeed, bytesWithNewSeed))
            }
        } finally {
            RandomSeederThread.remove(seedGenerator, prng)
            RandomSeederThread.stopIfEmpty(seedGenerator)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testStopIfEmpty() {
        val seedGenerator = FakeSeedGenerator("testStopIfEmpty")
        val prng = Random()
        RandomSeederThread.add(seedGenerator, prng)
        RandomSeederThread.stopIfEmpty(seedGenerator)
        assertTrue(RandomSeederThread.hasInstance(seedGenerator))
        RandomSeederThread.remove(seedGenerator, prng)
        RandomSeederThread.stopIfEmpty(seedGenerator)
        assertFalse(RandomSeederThread.hasInstance(seedGenerator))
    }

    @Test
    @Throws(Exception::class)
    fun testStopAllEmpty() {
        val neverAddedTo = FakeSeedGenerator("neverAddedTo")
        val addedToAndRemoved = FakeSeedGenerator("addedToAndRemoved")
        val addedToAndLeft = FakeSeedGenerator("addedToAndLeft")
        val addedAndRemoved = Random()
        val addedAndLeft = Random()
        RandomSeederThread.add(addedToAndRemoved, addedAndRemoved)
        RandomSeederThread.remove(addedToAndRemoved, addedAndRemoved)
        RandomSeederThread.add(addedToAndLeft, addedAndLeft)
        assertFalse(RandomSeederThread.hasInstance(neverAddedTo))
        assertTrue(RandomSeederThread.hasInstance(addedToAndRemoved))
        assertTrue(RandomSeederThread.hasInstance(addedToAndLeft))
        stopAllEmpty()
        assertFalse(RandomSeederThread.hasInstance(neverAddedTo))
        assertFalse(RandomSeederThread.hasInstance(addedToAndRemoved))
        assertTrue(RandomSeederThread.hasInstance(addedToAndLeft))
        addedAndLeft.nextInt() // prevent GC before this point
    }

    @Test
    fun testSetDefaultPriority() {
        RandomSeederThread.setDefaultPriority(7)
        try {
            val generator = FakeSeedGenerator("testSetDefaultPriority")
            val prng = Random()
            RandomSeederThread.add(generator, prng)
            try {
                var threadFound = false
                val threads = arrayOfNulls<Thread>(10 + Thread.activeCount())
                val nThreads = Thread.enumerate(threads)
                for (i in 0 until nThreads) {
                    if (threads[i] is RandomSeederThread && "RandomSeederThread for testSetDefaultPriority" == threads[i].getName()) {
                        assertEquals(threads[i].getPriority(), 7)
                        threadFound = true
                        break
                    }
                }
                assertTrue(threadFound, "Couldn't find the seeder thread!")
                prng.nextInt() // prevent GC before this point
            } finally {
                RandomSeederThread.remove(generator, prng)
                RandomSeederThread.stopIfEmpty(generator)
            }
        } finally {
            RandomSeederThread.setDefaultPriority(Thread.NORM_PRIORITY)
        }
    }

    @Test
    fun testSetPriority() {
        val prng = Random()
        val generator = FakeSeedGenerator("testSetPriority")
        RandomSeederThread.add(generator, prng)
        try {
            RandomSeederThread.setPriority(generator, 7)
            var threadFound = false
            val threads = arrayOfNulls<Thread>(10 + Thread.activeCount())
            val nThreads = Thread.enumerate(threads)
            for (i in 0 until nThreads) {
                if (threads[i] is RandomSeederThread && "RandomSeederThread for testSetPriority" == threads[i].getName()) {
                    assertEquals(threads[i].getPriority(), 7)
                    threadFound = true
                    break
                }
            }
            assertTrue(threadFound, "Couldn't find the seeder thread!")
        } finally {
            RandomSeederThread.remove(generator, prng)
            RandomSeederThread.stopIfEmpty(generator)
        }
    }

    companion object {

        private val TEST_SEED = 0x0123456789ABCDEFL
        private val TEST_OUTPUT_SIZE = 20

        private val ON_LINUX = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH)
                .contains("nux")
    }
}
