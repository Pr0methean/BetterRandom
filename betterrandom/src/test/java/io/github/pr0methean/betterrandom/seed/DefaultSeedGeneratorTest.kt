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

import org.testng.Assert.assertTrue

import java.security.Permission
import org.testng.annotations.Test

/**
 * Unit test for [DefaultSeedGenerator].
 * @author Daniel Dyer
 */
class DefaultSeedGeneratorTest : AbstractSeedGeneratorTest(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR) {

    @Test
    @Throws(SeedException::class)
    fun testBasicFunction() {
        SeedTestUtils.testGenerator(seedGenerator)
    }

    @Test
    fun testIsWorthTrying() {
        // Should always be true
        assertTrue(seedGenerator.isWorthTrying)
    }

    /**
     * Check that the default seed generator gracefully falls back to an alternative generation
     * strategy when the security manager prevents it from using its first choice.
     */
    @Test(timeOut = 120000)
    @Throws(SeedException::class)
    fun testRestrictedEnvironment() {
        val affectedThread = Thread.currentThread()
        val securityManager = System.getSecurityManager()
        try {
            // Don't allow file system or network access.
            System.setSecurityManager(RestrictedSecurityManager(affectedThread))
            seedGenerator.generateSeed(ByteArray(4))
            // Should get to here without exceptions.
        } finally {
            // Restore the original security manager so that we don't
            // interfere with the running of other tests.
            System.setSecurityManager(securityManager)
        }
    }

    /**
     * This security manager allows everything except for some operations that are explicitly blocked.
     * These operations are accessing /dev/random and opening a socket connection.
     */
    private class RestrictedSecurityManager private constructor(private val affectedThread: Thread) : SecurityManager() {

        override fun checkRead(file: String) {
            if (Thread.currentThread() === affectedThread && "/dev/random" == file) {
                throw SecurityException("Test not permitted to access /dev/random")
            }
        }

        override fun checkConnect(host: String, port: Int) {
            if (Thread.currentThread() === affectedThread) {
                throw SecurityException("Test not permitted to connect to " + host + ':'.toString() + port)
            }
        }

        override fun checkPermission(permission: Permission) {
            // Allow everything.
        }
    }
}
