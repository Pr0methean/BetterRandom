// ============================================================================
//   Copyright 2006-2012 Daniel W. Dyer
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
// ============================================================================
package io.github.pr0methean.betterrandom.seed

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * RNG seed strategy that gets data from `/dev/random` on systems that provide it (e.g.
 * Solaris/Linux).  If `/dev/random` does not exist or is not accessible, a [ ] is thrown. If it didn't exist during a previous call to this method or to [ ][.generateSeed], then for performance reasons, we assume for the rest of the JVM's lifespan
 * that it still doesn't exist.
 * @author Daniel Dyer
 */
enum class DevRandomSeedGenerator : SeedGenerator {

    /** Singleton instance.  */
    DEV_RANDOM_SEED_GENERATOR;

    override val isWorthTrying: Boolean
        get() = !DEV_RANDOM_DOES_NOT_EXIST.get()

    /**
     * @throws SeedException if /dev/random does not exist or is not accessible.
     */
    @Throws(SeedException::class)
    override fun generateSeed(
            randomSeed: ByteArray) {
        if (!isWorthTrying) {
            throw SeedException("$DEV_RANDOM_STRING did not exist when previously checked for")
        }

        try {
            if (inputStream == null) {
                synchronized(DevRandomSeedGenerator::class.java) {
                    if (inputStream == null) {
                        inputStream = FileInputStream(DEV_RANDOM)
                    }
                }
            }
            val length = randomSeed.size
            var count = 0
            while (count < length) {
                val bytesRead = inputStream!!.read(randomSeed, count, length - count)
                if (bytesRead == -1) {
                    throw SeedException("EOF encountered reading random data.")
                }
                count += bytesRead
            }
        } catch (ex: IOException) {
            if (!DEV_RANDOM.exists()) {
                LOG.error("{} does not exist", DEV_RANDOM_STRING)
                DEV_RANDOM_DOES_NOT_EXIST.lazySet(true)
            }
            throw SeedException("Failed reading from $DEV_RANDOM_STRING", ex)
        } catch (ex: SecurityException) {
            // Might be thrown if resource access is restricted (such as in
            // an applet sandbox).
            throw SeedException("SecurityManager prevented access to $DEV_RANDOM_STRING", ex)
        }

    }

    /** Returns "/dev/random".  */
    override fun toString(): String {
        return DEV_RANDOM_STRING
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(DevRandomSeedGenerator::class.java)
        private val DEV_RANDOM_STRING = "/dev/random"
        private val DEV_RANDOM = File(DEV_RANDOM_STRING)
        private val DEV_RANDOM_DOES_NOT_EXIST = AtomicBoolean(false)
        @Volatile
        private var inputStream: FileInputStream? = null
    }
}
