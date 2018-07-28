package io.github.pr0methean.betterrandom.seed

import io.github.pr0methean.betterrandom.TestUtils
import io.github.pr0methean.betterrandom.TestingDeficiency
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Proxy.Type
import java.util.UUID

/**
 * Utility methods used in [RandomDotOrgSeedGeneratorHermeticTest] and
 * [RandomDotOrgSeedGeneratorLiveTest].
 */
enum class RandomDotOrgUtils {
    ;

    companion object {

        private val TOR_PORT = 9050
        private val SMALL_REQUEST_SIZE = 32

        fun haveApiKey(): Boolean {
            return System.getenv("RANDOM_DOT_ORG_KEY") != null
        }

        fun setApiKey() {
            val apiKeyString = System.getenv("RANDOM_DOT_ORG_KEY")
            RandomDotOrgSeedGenerator
                    .setApiKey(if (apiKeyString == null) null else UUID.fromString(apiKeyString))
        }

        fun createTorProxy(): Proxy {
            return Proxy(Type.SOCKS, InetSocketAddress("localhost", TOR_PORT))
        }

        /**
         * Appveyor and the OSX environment on Travis-CI don't currently use enough IP addresses to get
         * heavy random.org usage allowed, so tests that are sufficiently demanding of random.org won't
         * run on those environments.
         * @return true if we're not running on Appveyor or a Travis-CI OSX instance, false if we are.
         */
        @TestingDeficiency
        fun canRunRandomDotOrgLargeTest(): Boolean {
            return !TestUtils.isAppveyor && "osx" != System.getenv("TRAVIS_OS_NAME")
        }

        internal fun maybeSetMaxRequestSize(): Boolean {
            if (!canRunRandomDotOrgLargeTest()) {
                RandomDotOrgSeedGenerator.setMaxRequestSize(SMALL_REQUEST_SIZE)
                return true
            }
            return false
        }
    }
}
