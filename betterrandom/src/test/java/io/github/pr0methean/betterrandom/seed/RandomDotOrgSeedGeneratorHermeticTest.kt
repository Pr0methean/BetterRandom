package io.github.pr0methean.betterrandom.seed

import io.github.pr0methean.betterrandom.seed.RandomDotOrgSeedGenerator.GLOBAL_MAX_REQUEST_SIZE
import io.github.pr0methean.betterrandom.seed.RandomDotOrgSeedGenerator.cache
import io.github.pr0methean.betterrandom.seed.RandomDotOrgSeedGenerator.cacheLock
import io.github.pr0methean.betterrandom.seed.RandomDotOrgSeedGenerator.cacheOffset
import io.github.pr0methean.betterrandom.seed.RandomDotOrgSeedGenerator.setProxy
import io.github.pr0methean.betterrandom.seed.RandomDotOrgUtils.createTorProxy
import io.github.pr0methean.betterrandom.seed.RandomDotOrgUtils.maybeSetMaxRequestSize
import io.github.pr0methean.betterrandom.seed.SeedTestUtils.testGenerator
import org.mockito.ArgumentMatchers.any
import org.powermock.api.mockito.PowerMockito.spy
import org.testng.Assert.assertTrue
import org.testng.AssertJUnit.assertEquals

import java.net.Proxy
import java.net.URL
import java.nio.charset.Charset
import java.util.UUID
import org.json.simple.parser.ParseException
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.mockpolicies.Slf4jMockPolicy
import org.powermock.core.classloader.annotations.MockPolicy
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.testng.PowerMockTestCase
import org.testng.Assert
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

@PowerMockIgnore("javax.management.*", "javax.script.*", "jdk.nashorn.*")
@MockPolicy(Slf4jMockPolicy::class)
@PrepareForTest(RandomDotOrgSeedGenerator::class)
@Test(singleThreaded = true)
class RandomDotOrgSeedGeneratorHermeticTest : PowerMockTestCase() {
    private var address: String? = null
    private val proxy = createTorProxy()
    private var usingSmallRequests = false

    @Throws(Exception::class)
    private fun mockRandomDotOrgResponse(response: ByteArray) {
        PowerMockito.doAnswer { invocationOnMock ->
            val url = invocationOnMock.getArgument<URL>(0)
            address = url.toString()
            FakeHttpsUrlConnection(url, null, response)
        }.`when`(RandomDotOrgSeedGenerator::class.java, "openConnection", any(URL::class.java))
    }

    @BeforeMethod
    fun setUpMethod() {
        spy(RandomDotOrgSeedGenerator::class.java)
        usingSmallRequests = maybeSetMaxRequestSize()
        assertTrue(cacheLock.tryLock()) // If this blocks, then our tests risk interfering
        try {
            cacheOffset = cache.size
        } finally {
            cacheLock.unlock()
        }
    }

    @Test
    @Throws(Exception::class)
    fun testSetProxyOldApi() {
        setProxy(proxy)
        mockRandomDotOrgResponse(if (usingSmallRequests) RESPONSE_32_OLD_API else RESPONSE_625_OLD_API)
        RandomDotOrgSeedGenerator.setApiKey(null)
        try {
            testGenerator(RandomDotOrgSeedGenerator.RANDOM_DOT_ORG_SEED_GENERATOR)
            assertTrue(address!!.startsWith("https://www.random.org/integers"))
            assertEquals(proxy, RandomDotOrgSeedGenerator.proxy.get())
        } finally {
            setProxy(null)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testSetProxyJsonApi() {
        RandomDotOrgSeedGenerator.setApiKey(UUID.randomUUID())
        setProxy(proxy)
        mockRandomDotOrgResponse(if (usingSmallRequests) RESPONSE_32_JSON else RESPONSE_625_JSON)
        try {
            testGenerator(RandomDotOrgSeedGenerator.RANDOM_DOT_ORG_SEED_GENERATOR)
            assertTrue(address!!.startsWith("https://api.random.org/json-rpc/1/invoke"))
            assertEquals(proxy, RandomDotOrgSeedGenerator.proxy.get())
        } finally {
            setProxy(null)
            RandomDotOrgSeedGenerator.setApiKey(null)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testOverLongResponse() {
        RandomDotOrgSeedGenerator.setApiKey(null)
        RandomDotOrgSeedGenerator.setMaxRequestSize(32)
        mockRandomDotOrgResponse(RESPONSE_625_OLD_API)
        try {
            testGenerator(RandomDotOrgSeedGenerator.RANDOM_DOT_ORG_SEED_GENERATOR)
        } finally {
            if (!usingSmallRequests) {
                RandomDotOrgSeedGenerator.setMaxRequestSize(GLOBAL_MAX_REQUEST_SIZE)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testOverShortResponse() {
        RandomDotOrgSeedGenerator.setApiKey(null)
        RandomDotOrgSeedGenerator.setMaxRequestSize(GLOBAL_MAX_REQUEST_SIZE)
        mockRandomDotOrgResponse(RESPONSE_32_OLD_API)
        try {
            expectAndGetException()
        } finally {
            maybeSetMaxRequestSize()
        }
    }

    @Test
    @Throws(Exception::class)
    fun testInvalidResponseJsonApi() {
        RandomDotOrgSeedGenerator.setApiKey(UUID.randomUUID())
        try {
            mockRandomDotOrgResponse("Not JSON".toByteArray(UTF8))
            assertTrue(expectAndGetException().cause is ParseException)
        } finally {
            RandomDotOrgSeedGenerator.setApiKey(null)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testInvalidResponseOldApi() {
        RandomDotOrgSeedGenerator.setApiKey(null)
        mockRandomDotOrgResponse("Not numbers".toByteArray(UTF8))
        assertTrue(expectAndGetException().cause is NumberFormatException)
    }

    @Test
    @Throws(Exception::class)
    fun testResponseError() {
        RandomDotOrgSeedGenerator.setApiKey(UUID.randomUUID())
        try {
            mockRandomDotOrgResponse(("{\"jsonrpc\":\"2.0\",\"error\":\"Oh noes, an error\",\"result\":{\"random\":{\"data\":"
                    + "[\"gAlhFSSjLy+u5P/Cz92BH4R3NZ0+j8UHNeIR02CChoQ=\"]," + "\"completionTime\":\"2018-05-06 19:54:31Z\"},\"bitsUsed\":256,\"bitsLeft\":996831,"
                    + "\"requestsLeft\":199912,\"advisoryDelay\":290},\"id\":27341}").toByteArray(UTF8))
            assertEquals("Wrong exception message", "Oh noes, an error", expectAndGetException().message)
        } finally {
            RandomDotOrgSeedGenerator.setApiKey(null)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testResponseNoResult() {
        mockRandomDotOrgResponse("{\"jsonrpc\":\"2.0\"}".toByteArray(UTF8))
        expectAndGetException()
    }

    @Test
    @Throws(Exception::class)
    fun testResponseNoRandom() {
        mockRandomDotOrgResponse(("{\"jsonrpc\":\"2.0\",\"result\":{"
                + "\"completionTime\":\"2018-05-06 19:54:31Z\"},\"bitsUsed\":256,\"bitsLeft\":996831,"
                + "\"requestsLeft\":199912,\"advisoryDelay\":290},\"id\":27341}").toByteArray(UTF8))
        expectAndGetException()
    }

    @AfterMethod
    fun tearDownMethod() {
        address = null
    }

    companion object {

        private val UTF8 = Charset.forName("UTF-8")
        val RESPONSE_32_JSON = ("{\"jsonrpc\":\"2.0\",\"result\":{\"random\":{\"data\":"
                + "[\"gAlhFSSjLy+u5P/Cz92BH4R3NZ0+j8UHNeIR02CChoQ=\"],"
                + "\"completionTime\":\"2018-05-06 19:54:31Z\"},\"bitsUsed\":256,\"bitsLeft\":996831,"
                + "\"requestsLeft\":199912,\"advisoryDelay\":290},\"id\":27341}").toByteArray(UTF8)
        val RESPONSE_32_OLD_API = ("19\ne0\ne9\n6b\n85\nbf\na5\n07\na7\ne9\n65\n2e\n90\n42\naa\n02\n2d\nc4\n67\n2a\na3\n32\n" + "9d\nbc\nd1\n9b\n2f\n7c\nf3\n60\n30\ne5").toByteArray(UTF8)
        val RESPONSE_625_JSON = ("{\"jsonrpc\":\"2.0\",\"result\":{\"random\":{\"data\":"
                + "[\"Tcx+z1Q4AN4j9IGSNkAPl38/Tfh16WW+1p6Gbbss/wenciNlyaBneI8PyHNB5m3/oKj9M9F+PEbF"
                + "uPNsupWjx5YIHxkSlFJo7emuQJ0NLScDBT+mMKLc58FwEpu0i+tklbm3pXctSuZvJ68In7HZGe29"
                + "5rhwXdRiB4JCEkE214RQlS3bSYGnxGODjvHxiwwR80VLTLUZe6sFlpeY1fcuzn3K+fmO2eVyMdKe"
                + "gL/74nTe6u9R6hGQ7AehX8NP7dEKhUaTVv43NGcXCOX6rdBzo6U82CsQKkNHXa8FhJLDOSlFENhH"
                + "B6eI/WZfImtbxFLFSr6PI8A92472kK1bDqTOlvRPQAjLc1T1yuEW6tsA9z3Wfm0YozojtUYc0xED"
                + "XP2rf89oHsK5wni4w1qkY/VYAdjWv2vZjSXNZ/c/qRli7pk7/hG3oXPoZ5GpLfuv7k+hRgD7Zcr+"
                + "JNbLSlSUtGQtNmK18s/mhrvmmE3brk/wGpdqaBeOs6SFmBZf1iSAFzn9RPjhWfZIBsUY0YSb8RWy"
                + "oBV7acIfXzKRUyE7cfqSGnrsIqcr7FWHXe3kodOF0nzfOsZGzx+sL0/GsRzW/WahlxtleQyatmyP"
                + "/JJ/3c5UiLVY58+hkJdokqtDGe/2F1itMAGPOiCcfUc+O1dtf9YEgLmaYpEupkAvgaGop2/qDh+i"
                + "XcH3ShjCQVsVOmTzw6AharEQYcz8sML+pu12LusAJc61sKZ9TamddrpKljmH2liB6GFs8DD7DyFB"
                + "V/7ORy6SWbejQd2wQ2fz2UAJ1aZME/ODGgYCWXPQOTMNcl+eaF2ubUf7BI2G0w+xP7tbum7GRQ==\"],"
                + "\"completionTime\":\"2018-05-06 19:54:31Z\"},\"bitsUsed\":256,\"bitsLeft\":996831,"
                + "\"requestsLeft\":199912,\"advisoryDelay\":290},\"id\":27341}").toByteArray(UTF8)
        val RESPONSE_625_OLD_API = ("de\ne9\n61\n91\n1c\nab\n89\n29\n3b\n87\n93\n3b\n79\n01\naa\n95\n56\n6a\nf0\n2f\n73\n32\n71\n32\n1f\n45\n29\nf7\n0d\n48\n"
                + "cc\n63\n69\n95\nd4\nf6\n8a\n3a\ne2\na9\n51\n54\nc7\nca\nd2\n17\n56\nd8\n1d\n26\na9\n23\n28\nce\nfe\nae\nc3\n8c\nf2\na1\n"
                + "25\n6a\n7b\n5e\nf1\n1b\n25\nff\n93\n12\n49\nde\n79\n53\n85\nc1\nd1\n11\ne8\n6b\nd3\naf\n01\n0d\n83\nf8\n47\nc5\n2e\n44\n"
                + "07\n10\ncf\nb3\ne6\n75\n49\n39\ncf\n7b\n24\naf\n58\n6c\n40\n37\n9e\nc0\nb5\ncf\n3b\n1d\ne6\nb7\n93\n79\n66\n9d\n3c\nbe\n"
                + "0c\nf4\n21\ne4\n69\n1d\n17\n3a\n85\n20\nd2\n34\n3e\nb9\n9d\n3a\n8f\n3c\ne7\n8b\n86\n0d\n08\ne2\n99\n5b\n6a\ncc\n0c\ndf\n"
                + "e2\n54\n18\n9e\n6e\n01\n63\n34\n0e\n50\n9e\n58\n67\n7f\naa\n56\n3e\ne0\naf\n0d\n6e\n30\n1a\n5b\n12\n14\nc8\n2c\n8f\n40\n"
                + "13\ncf\n05\n27\n6f\n81\ne9\n21\na2\n64\nf1\nd3\n17\nd0\n95\nbb\n52\n27\n2d\na7\ndc\n89\ne1\nc8\n84\nd7\n20\ndf\nb2\nc2\n"
                + "fc\n52\n50\nc9\n30\n16\n63\n10\ncb\nf2\nf4\nba\nbb\nac\n62\n5f\n0e\ndc\n24\n1f\n42\n71\nf4\n03\na0\n3e\ndf\neb\n60\n76\n"
                + "51\nd5\n6b\ncd\n50\n38\nd8\n69\n25\nf4\nd7\n77\n65\n14\n37\nd3\nea\n4a\nab\nae\n92\n13\n9d\nad\nfe\n90\n19\nf9\n2b\n83\n"
                + "be\n01\n11\n37\na6\n32\nf6\n13\n25\na1\n79\ncb\n01\n07\ne7\n28\n42\n56\n57\n7b\n01\nc8\n31\nd8\n72\na3\na4\nc2\n30\nbd\n"
                + "0d\nf6\n32\ncd\na5\nfc\nca\n29\n2a\n0a\nfc\n59\n40\n90\nc0\n05\n16\n66\n64\n5b\ne3\n76\nfb\n1d\n34\n4d\nc7\n4c\n06\nc0\n"
                + "40\nf0\n02\nc0\n6f\ne8\n65\ne7\nb2\nee\n31\n39\nf5\n7e\n19\nef\nc1\n8e\n3b\n0c\naa\n48\naf\n6a\n7c\nae\n60\n66\n8d\n87\n"
                + "c4\n27\n15\nb4\n70\ne7\n96\n15\n4a\n41\n03\n74\na9\n05\nc8\n75\n51\nd4\ne3\n8c\n28\nd1\n4f\na2\nb0\nb0\n7f\n08\nd4\ndf\n"
                + "ad\nee\nb8\ne8\ndd\n29\n8b\n51\n36\ne8\nc0\n7c\nd7\n8f\n21\nfe\n64\n6a\n49\n6b\n63\na7\n26\n62\n33\n7c\n29\n35\n33\n9a\n"
                + "d5\n80\na2\n31\nf6\n8c\n21\nb5\n18\n47\n53\n26\n2c\ncf\na4\ned\n4f\n60\n5d\nc0\nbd\nb9\n44\ne4\n13\ne2\n3a\n7b\nb2\n27\n"
                + "9d\n58\nb0\n18\nb6\n5e\na8\n22\n5b\n5a\ne6\n10\nd2\n24\n1f\n22\nbd\nf1\n80\n3e\n36\n06\ne4\nd8\n03\n34\nbd\nff\n1d\nd5\n"
                + "dc\n21\n08\nc2\n84\nc5\n3e\nc4\n1c\n4c\n16\n99\n9d\n22\nf3\nf2\nab\n5a\nd1\n51\n42\n8f\nb8\nf5\nbe\n4e\nd0\n83\n6a\n73\n"
                + "19\nc3\nde\n91\n9f\n1a\n1a\n62\n08\n60\nae\n7c\n67\n25\na4\n99\ne2\n14\n00\n6b\n89\n47\nec\n9d\nd6\neb\nb2\n54\n44\n7f\n"
                + "1b\ndd\n40\ne7\nf1\n93\nd8\nf2\n9d\n67\n39\n9f\ne2\n0c\ndd\n61\n94\n05\n36\n3c\n07\ne8\n17\nc1\n05\n9c\nf3\nfd\n72\n3d\n"
                + "85\n05\n86\n4d\nea\n40\n17\nd1\n9e\ncc\n42\nf5\ndb\n9c\na6\n98\nb9\n93\n4e\n36\nf6\nb5\n94\nb3\n1e\n73\nf6\n2c\n12\nad\n"
                + "71\n93\n1e\n6c\n7c\n23\nd8\nfa\n4c\n24\nc5\nb4\n3a\nd4\ne0\nf8\n9c\n4a\ne6\n15\n97\n5d\n48\na0\n0d")
                .toByteArray(UTF8)

        private fun expectAndGetException(): SeedException {
            var exception: SeedException? = null
            try {
                RandomDotOrgSeedGenerator.RANDOM_DOT_ORG_SEED_GENERATOR.generateSeed(
                        SeedTestUtils.SEED_SIZE)
                Assert.fail("Should have thrown SeedException")
            } catch (expected: SeedException) {
                exception = expected
            }

            return exception
        }
    }
}
