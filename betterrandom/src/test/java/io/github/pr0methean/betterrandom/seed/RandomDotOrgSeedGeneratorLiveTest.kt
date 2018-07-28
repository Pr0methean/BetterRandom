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

import io.github.pr0methean.betterrandom.seed.RandomDotOrgSeedGenerator.setProxy
import io.github.pr0methean.betterrandom.seed.RandomDotOrgUtils.canRunRandomDotOrgLargeTest
import io.github.pr0methean.betterrandom.seed.RandomDotOrgUtils.haveApiKey
import org.testng.Assert.assertEquals

import java.net.InetAddress
import java.net.Proxy
import java.net.UnknownHostException
import org.testng.Assert
import org.testng.Reporter
import org.testng.SkipException
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

/**
 * Unit test for the seed generator that connects to random.org to get seed data.
 * @author Daniel Dyer
 * @author Chris Hennick
 */
@Test(singleThreaded = true)
class RandomDotOrgSeedGeneratorLiveTest : AbstractSeedGeneratorTest(RandomDotOrgSeedGenerator.RANDOM_DOT_ORG_SEED_GENERATOR) {

    protected val proxy = RandomDotOrgUtils.createTorProxy()

    @Test(timeOut = 120000)
    @Throws(SeedException::class)
    fun testGeneratorOldApi() {
        if (canRunRandomDotOrgLargeTest()) {
            RandomDotOrgSeedGenerator.setApiKey(null)
            SeedTestUtils.testGenerator(seedGenerator)
        } else {
            throw SkipException("Test can't run on this platform")
        }
    }

    @Test(timeOut = 120000)
    @Throws(SeedException::class)
    fun testGeneratorNewApi() {
        if (canRunRandomDotOrgLargeTest() && haveApiKey()) {
            RandomDotOrgUtils.setApiKey()
            SeedTestUtils.testGenerator(seedGenerator)
        } else {
            throw SkipException("Test can't run on this platform")
        }
    }

    /**
     * Try to acquire a large number of bytes, more than are cached internally by the seed generator
     * implementation.
     */
    @Test(timeOut = 120000)
    @Throws(SeedException::class)
    fun testLargeRequest() {
        if (canRunRandomDotOrgLargeTest()) {
            RandomDotOrgUtils.setApiKey()
            // Request more bytes than are cached internally.
            val seedLength = 626
            assertEquals(seedGenerator.generateSeed(seedLength).size, seedLength,
                    "Failed to generate seed of length $seedLength")
        } else {
            throw SkipException("Test can't run on this platform")
        }
    }

    override fun testToString() {
        super.testToString()
        Assert.assertNotNull(RandomDotOrgSeedGenerator.DELAYED_RETRY.toString())
    }

    @Test
    @Throws(Exception::class)
    fun testSetProxyReal() {
        if (!canRunRandomDotOrgLargeTest()) {
            throw SkipException("Test can't run on this platform")
        }
        setProxy(proxy)
        try {
            SeedTestUtils.testGenerator(seedGenerator)
        } finally {
            setProxy(null)
        }
    }

    @BeforeClass
    fun setUpClass() {
        RandomDotOrgUtils.maybeSetMaxRequestSize()
        // when using Tor, DNS seems to be unreliable, so it may take several tries to get the address
        var address: InetAddress? = null
        var failedLookups: Long = 0
        while (address == null) {
            try {
                address = InetAddress.getByName("api.random.org")
            } catch (e: UnknownHostException) {
                failedLookups++
            }

        }
        if (failedLookups > 0) {
            Reporter.log("Failed to look up api.random.org address on the first $failedLookups attempts")
        }
    }

    @AfterMethod
    fun tearDownMethod() {
        RandomDotOrgSeedGenerator.setApiKey(null)
    }

}
