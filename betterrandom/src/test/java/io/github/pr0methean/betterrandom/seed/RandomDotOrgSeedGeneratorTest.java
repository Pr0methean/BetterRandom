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
package io.github.pr0methean.betterrandom.seed;

import static io.github.pr0methean.betterrandom.TestUtils.canRunRandomDotOrgLargeTest;
import static io.github.pr0methean.betterrandom.TestUtils.isAppveyor;
import static io.github.pr0methean.betterrandom.seed.RandomDotOrgSeedGenerator.setProxy;
import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.UUID;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Unit test for the seed generator that connects to random.org to get seed data.
 * @author Daniel Dyer
 * @author Chris Hennick
 */
@PrepareForTest(URL.class)
@Test(singleThreaded = true)
public class RandomDotOrgSeedGeneratorTest extends AbstractSeedGeneratorTest {

  private static final int SMALL_REQUEST_SIZE = 32;
  private static final int TOR_PORT = 9050;
  private static final Charset UTF8 = Charset.forName("UTF-8");
  @SuppressWarnings({"HardcodedFileSeparator", "HardcodedLineSeparator"})
  private static final byte[] RESPONSE_32 = haveApiKey()
      ? ("{\"jsonrpc\":\"2.0\",\"result\":{\"random\":{\"data\":"
          + "[\"gAlhFSSjLy+u5P/Cz92BH4R3NZ0+j8UHNeIR02CChoQ=\"],"
          + "\"completionTime\":\"2018-05-06 19:54:31Z\"},\"bitsUsed\":256,\"bitsLeft\":996831,"
          + "\"requestsLeft\":199912,\"advisoryDelay\":290},\"id\":27341}").getBytes(UTF8)
      : ("19\ne0\ne9\n6b\n85\nbf\na5\n07\na7\ne9\n65\n2e\n90\n42\naa\n02\n2d\nc4\n67\n2a\na3\n32\n"
          + "9d\nbc\nd1\n9b\n2f\n7c\nf3\n60\n30\ne5").getBytes(UTF8);
  private Proxy proxy;
  private final URL mockUrl = mock(URL.class);
  private String[] address = {null};

  public RandomDotOrgSeedGeneratorTest() {
    super(RandomDotOrgSeedGenerator.RANDOM_DOT_ORG_SEED_GENERATOR);
  }

  private static boolean haveApiKey() {
    return System.getenv("RANDOM_DOT_ORG_KEY") != null;
  }

  private static void setApiKey() {
    final String apiKeyString = System.getenv("RANDOM_DOT_ORG_KEY");
    RandomDotOrgSeedGenerator
        .setApiKey((apiKeyString == null) ? null : UUID.fromString(apiKeyString));
  }

  @BeforeClass public void setUp() {
    proxy = /* FIXME once Appveyor adds proxies for all its IPs:
        isAppveyor()
        ? new Proxy(Type.HTTP,
            new InetSocketAddress(System.getenv("APPVEYOR_HTTP_PROXY_IP"),
            Integer.valueOf(System.getenv("APPVEYOR_HTTP_PROXY_PORT"))))
        : */ new Proxy(Type.SOCKS, new InetSocketAddress("localhost", TOR_PORT));
    if (!canRunRandomDotOrgLargeTest()) {
      RandomDotOrgSeedGenerator.setMaxRequestSize(SMALL_REQUEST_SIZE);
    }
  }

  @AfterMethod
  public void tearDown() {
    RandomDotOrgSeedGenerator.setApiKey(null);
  }

  @Test(timeOut = 120000) public void testGeneratorOldApi() throws SeedException {
    if (canRunRandomDotOrgLargeTest()) {
      RandomDotOrgSeedGenerator.setApiKey(null);
      SeedTestUtils.testGenerator(seedGenerator);
    } else {
      throw new SkipException("Test can't run on this platform");
    }
  }

  @Test(timeOut = 120000) public void testGeneratorNewApi() throws SeedException {
    if (canRunRandomDotOrgLargeTest()) {
      setApiKey();
      SeedTestUtils.testGenerator(seedGenerator);
    } else {
      throw new SkipException("Test can't run on this platform");
    }
  }

  /**
   * Try to acquire a large number of bytes, more than are cached internally by the seed generator
   * implementation.
   */
  @Test(timeOut = 120000) public void testLargeRequest() throws SeedException {
    if (canRunRandomDotOrgLargeTest()) {
      setApiKey();
      // Request more bytes than are cached internally.
      final int seedLength = 626;
      assertEquals(seedGenerator.generateSeed(seedLength).length, seedLength,
          "Failed to generate seed of length " + seedLength);
    }
  }

  @Override public void testToString() {
    super.testToString();
    Assert.assertNotNull(RandomDotOrgSeedGenerator.DELAYED_RETRY.toString());
  }

  @Test
  public void testSetProxyHermetic() throws Exception {
    setProxy(proxy);
    try {
      enableMockUrl();
      FakeHttpsUrlConnection[] connection = {null};
      when(mockUrl.openConnection(any(Proxy.class))).thenAnswer(invocationOnMock -> {
        assertSame(proxy, invocationOnMock.getArgument(0));
        connection[0] = new FakeHttpsUrlConnection(mockUrl, proxy, RESPONSE_32);
        return connection[0];
      });
      SeedTestUtils.testGenerator(seedGenerator);
    } finally {
      setProxy(null);
    }
  }

  private void enableMockUrl() throws Exception {
    PowerMockito.whenNew(URL.class).withAnyArguments().thenAnswer(invocationOnMock -> {
      address[0] = invocationOnMock.getArgument(0);
      return mockUrl;
    });
  }

  @Test
  public void testSetProxyReal() throws Exception {
    if (!canRunRandomDotOrgLargeTest()) {
      throw new SkipException("Test can't run on this platform");
    }
    setProxy(proxy);
    try {
      SeedTestUtils.testGenerator(seedGenerator);
    } finally {
      setProxy(null);
    }
  }
}
