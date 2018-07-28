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

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.Proxy
import java.net.URL
import java.nio.charset.Charset
import java.text.MessageFormat
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Base64.Decoder
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException
import org.slf4j.LoggerFactory

/**
 *
 * Connects to [random.org's old
 * API](https://www.random.org/clients/http/) (via HTTPS) and downloads a set of random bits to use as seed data.  It is generally
 * better to use the [DevRandomSeedGenerator] where possible, as it should be much quicker.
 * This seed generator is most useful on Microsoft Windows without Cygwin, and other platforms that
 * do not provide /dev/random.
 *
 * Random.org collects randomness from atmospheric noise using 9 radios, located at undisclosed
 * addresses in Dublin and Copenhagen and tuned to undisclosed AM/FM frequencies. (The secrecy is
 * intended to help prevent tampering with the output using a well-placed radio transmitter, and the
 * use of AM/FM helps ensure that any such tampering would cause illegal interference with
 * broadcasts and quickly attract regulatory attention.)
 *
 * Random.org has two APIs: an [old API](https://www.random.org/clients/http/) and a
 * [newer JSON-RPC API](https://api.random.org/json-rpc/1/). Since the new one requires
 * a key obtained from random.org, the old one is used by default. However, if you have a key, you
 * can provide it by calling [.setApiKey], and the new API will then be used.
 *
 * Note that when using the old API, random.org limits the supply of free random numbers to any
 * one IP address; if you operate from a fixed address (at least if you use IPv4), you can [check
 * your quota and buy more](https://www.random.org/quota/). On the new API, the quota is per key rather than per IP, and
 * commercial service tiers are to come in early 2018, shortly after the new API leaves beta.
 * @author Daniel Dyer (old API)
 * @author Chris Hennick (new API)
 */
enum class RandomDotOrgSeedGenerator private constructor(
        /**
         * If true, don't attempt to contact random.org again for RETRY_DELAY after an IOException
         */
        private val useRetryDelay: Boolean) : SeedGenerator {
    /**
     * This version of the client may make HTTP requests as fast as your computer is capable of
     * sending them. Since it is inherently spammy, it is recommended only when you know your usage is
     * light and/or no other source of randomness will do.
     */
    RANDOM_DOT_ORG_SEED_GENERATOR(false),

    /**
     * Upon a failed request, this version of the client waits 10 seconds before trying again. If
     * called again during that waiting period, throws [SeedException]. The [ ] uses this version.
     */
    DELAYED_RETRY(true);

    override val isWorthTrying: Boolean
        get() = !useRetryDelay || !earliestNextAttempt.isAfter(CLOCK.instant())

    @Throws(SeedException::class)
    override fun generateSeed(seedData: ByteArray) {
        if (!isWorthTrying) {
            throw SeedException("Not retrying so soon after an IOException")
        }
        val length = seedData.size
        cacheLock.lock()
        try {
            var count = 0
            while (count < length) {
                if (cacheOffset < cache.size) {
                    val numberOfBytes = Math.min(length - count, cache.size - cacheOffset)
                    System.arraycopy(cache, cacheOffset, seedData, count, numberOfBytes)
                    count += numberOfBytes
                    cacheOffset += numberOfBytes
                } else {
                    refreshCache(length - count)
                }
            }
        } catch (ex: IOException) {
            earliestNextAttempt = CLOCK.instant().plus(RETRY_DELAY)
            throw SeedException("Failed downloading bytes from $BASE_URL", ex)
        } catch (ex: SecurityException) {
            // Might be thrown if resource access is restricted (such as in an applet sandbox).
            throw SeedException("SecurityManager prevented access to $BASE_URL", ex)
        } finally {
            cacheLock.unlock()
        }
    }

    /**
     * Returns "https://www.random.org (with retry delay)" or "https://www.random.org (without retry
     * delay)".
     */
    override fun toString(): String {
        return BASE_URL + if (useRetryDelay) " (with retry delay)" else " (without retry delay)"
    }

    companion object {
        private val JSON_REQUEST_FORMAT = "{\"jsonrpc\":\"2.0\"," + "\"method\":\"generateBlobs\",\"params\":{\"apiKey\":\"%s\",\"n\":1,\"size\":%d},\"id\":%d}"

        private val REQUEST_ID = AtomicLong(0)
        private val API_KEY = AtomicReference<UUID>(null)
        private val JSON_PARSER = JSONParser()
        private val BASE64 = Base64.getDecoder()
        /**
         * Measures the retry delay. A ten-second delay might become either nothing or an hour if we used
         * local time during the start or end of Daylight Saving Time, but it's fine if we occasionally
         * wait 9 or 11 seconds instead of 10 because of a leap-second adjustment. See [Tom Scott's video](https://www.youtube.com/watch?v=-5wpm-gesOY) about the various
         * considerations involved in this choice of clock.
         */
        private val CLOCK = Clock.systemUTC()
        private val MAX_CACHE_SIZE = 625 // 5000 bits = 1/50 daily limit per API key
        private val BASE_URL = "https://www.random.org"
        /**
         * The URL from which the random bytes are retrieved (old API).
         */
        private val RANDOM_URL = "$BASE_URL/integers/?num={0,number,0}&min=0&max=255&col=1&base=16&format=plain&rnd=new"
        /**
         * Used to identify the client to the random.org service.
         */
        private val USER_AGENT = RandomDotOrgSeedGenerator::class.java.name
        /**
         * Random.org does not allow requests for more than 10k integers at once. This field is
         * package-visible for testing.
         */
        internal val GLOBAL_MAX_REQUEST_SIZE = 10000
        private val RETRY_DELAY = Duration.ofSeconds(10)
        internal val cacheLock: Lock = ReentrantLock()
        private val UTF8 = Charset.forName("UTF-8")
        @Volatile
        private var earliestNextAttempt = Instant.MIN
        @Volatile
        internal var cache = ByteArray(MAX_CACHE_SIZE)
        @Volatile
        internal var cacheOffset = cache.size
        @Volatile
        private var maxRequestSize = GLOBAL_MAX_REQUEST_SIZE
        private val JSON_REQUEST_URL: URL
        /**
         * The proxy to use with random.org, or null to use the JVM default. Package-visible for testing.
         */
        internal val proxy = AtomicReference<Proxy>(null)

        init {
            try {
                JSON_REQUEST_URL = URL("https://api.random.org/json-rpc/1/invoke")
            } catch (e: MalformedURLException) {
                // Should never happen.
                throw RuntimeException(e)
            }

        }

        /**
         * Sets the API key. If not null, random.org's JSON API is used. Otherwise, the old API is used.
         * @param apiKey An API key obtained from random.org.
         */
        fun setApiKey(apiKey: UUID?) {
            API_KEY.set(apiKey)
        }

        /**
         * Sets the proxy to use to connect to random.org. If null, the JVM default is used.
         * @param proxy a proxy, or null for the JVM default
         */
        fun setProxy(proxy: Proxy?) {
            RandomDotOrgSeedGenerator.proxy.set(proxy)
        }

        /* Package-visible for testing. */
        @Throws(IOException::class)
        internal fun openConnection(url: URL): HttpURLConnection {
            val currentProxy = proxy.get()
            return (if (currentProxy == null) url.openConnection() else url.openConnection(currentProxy)) as HttpURLConnection
        }

        /**
         * @param requiredBytes The preferred number of bytes to request from random.org. The
         * implementation may request more and cache the excess (to avoid making lots of small
         * requests). Alternatively, it may request fewer if the required number is greater than that
         * permitted by random.org for a single request.
         * @throws IOException If a connection error occurs.
         * @throws SeedException If random.org sends a malformed response body.
         */
        @Throws(IOException::class)
        private fun refreshCache(
                requiredBytes: Int) {
            var connection: HttpURLConnection? = null
            cacheLock.lock()
            try {
                var numberOfBytes = Math.max(requiredBytes, cache.size)
                numberOfBytes = Math.min(numberOfBytes, maxRequestSize)
                if (numberOfBytes != cache.size) {
                    cache = ByteArray(numberOfBytes)
                    cacheOffset = numberOfBytes
                }
                val currentApiKey = API_KEY.get()
                if (currentApiKey == null) {
                    // Use old API.
                    connection = openConnection(URL(MessageFormat.format(RANDOM_URL, numberOfBytes)))
                    connection.setRequestProperty("User-Agent", USER_AGENT)
                    BufferedReader(
                            InputStreamReader(connection.inputStream)).use { reader ->
                        var index = -1
                        var line: String? = reader.readLine()
                        while (line != null) {
                            ++index
                            if (index >= numberOfBytes) {
                                LoggerFactory.getLogger(RandomDotOrgSeedGenerator::class.java)
                                        .warn("random.org sent more data than requested.")
                                break
                            }
                            try {
                                cache[index] = Integer.parseInt(line, 16).toByte()
                                // Can't use Byte.parseByte, since it expects signed
                            } catch (e: NumberFormatException) {
                                throw SeedException("random.org sent non-numeric data", e)
                            }

                            line = reader.readLine()
                        }
                        if (index < cache.size - 1) {
                            throw SeedException(String
                                    .format("Insufficient data received: expected %d bytes, got %d.", cache.size,
                                            index + 1))
                        }
                    }
                } else {
                    // Use JSON API.
                    connection = openConnection(JSON_REQUEST_URL)
                    connection.doOutput = true
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("User-Agent", USER_AGENT)
                    connection.outputStream.use { out ->
                        out.write(String.format(JSON_REQUEST_FORMAT, currentApiKey, numberOfBytes * java.lang.Byte.SIZE,
                                REQUEST_ID.incrementAndGet()).toByteArray(UTF8))
                    }
                    var response: JSONObject
                    try {
                        connection.inputStream.use { `in` -> InputStreamReader(`in`).use { reader -> response = JSON_PARSER.parse(reader) as JSONObject } }
                    } catch (e: ParseException) {
                        throw SeedException("Unparseable JSON response from random.org", e)
                    }

                    val error = response["error"]
                    if (error != null) {
                        throw SeedException(error.toString())
                    }
                    val result = checkedGetObject(response, "result")
                    val random = checkedGetObject(result, "random")
                    val data = random["data"]
                    if (data == null) {
                        throw SeedException("'data' missing from 'random': $random")
                    } else {
                        val base64seed = (if (data is JSONArray) data[0] else data).toString()
                        val decodedSeed = BASE64.decode(base64seed)
                        if (decodedSeed.size < numberOfBytes) {
                            throw SeedException(String.format(
                                    "Too few bytes returned: expected %d bytes, got '%s'", numberOfBytes, base64seed))
                        }
                        System.arraycopy(decodedSeed, 0, cache, 0, numberOfBytes)
                    }
                    val advisoryDelayMs = result["advisoryDelay"] as Number
                    if (advisoryDelayMs != null) {
                        val advisoryDelay = Duration.ofMillis(advisoryDelayMs.toLong())
                        // Wait RETRY_DELAY or the advisory delay, whichever is shorter
                        earliestNextAttempt = CLOCK.instant()
                                .plus(if (advisoryDelay.compareTo(RETRY_DELAY) > 0) RETRY_DELAY else advisoryDelay)
                    }
                }
                cacheOffset = 0
            } finally {
                cacheLock.unlock()
                if (connection != null) {
                    connection.disconnect()
                }
            }
        }

        private fun checkedGetObject(parent: JSONObject, key: String): JSONObject {
            return parent[key] as JSONObject ?: throw SeedException("No '$key' in: $parent")
        }

        /**
         * Sets the maximum request size that we will expect random.org to allow. If more than [ ][.GLOBAL_MAX_REQUEST_SIZE], will be set to that value instead.
         * @param maxRequestSize the new maximum request size in bytes.
         */
        fun setMaxRequestSize(maxRequestSize: Int) {
            var maxRequestSize = maxRequestSize
            maxRequestSize = Math.min(maxRequestSize, GLOBAL_MAX_REQUEST_SIZE)
            val maxNewCacheSize = Math.min(maxRequestSize, MAX_CACHE_SIZE)
            cacheLock.lock()
            try {
                val sizeChange = maxNewCacheSize - cache.size
                if (sizeChange > 0) {
                    val newCache = ByteArray(maxNewCacheSize)
                    val newCacheOffset = cacheOffset + sizeChange
                    System.arraycopy(cache, cacheOffset, newCache, newCacheOffset, cache.size - cacheOffset)
                    cache = newCache
                    cacheOffset = newCacheOffset
                }
                RandomDotOrgSeedGenerator.maxRequestSize = maxRequestSize
            } finally {
                cacheLock.unlock()
            }
        }
    }
}
