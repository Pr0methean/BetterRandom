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
import static org.testng.Assert.assertEquals;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.UUID;
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
@Test(singleThreaded = true)
public class RandomDotOrgSeedGeneratorTest extends AbstractSeedGeneratorTest {

  private static final int SMALL_REQUEST_SIZE = 32;
  private static final int TOR_PORT = 9050;
  private Proxy proxy;

  public RandomDotOrgSeedGeneratorTest() {
    super(RandomDotOrgSeedGenerator.RANDOM_DOT_ORG_SEED_GENERATOR);
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
  public void testSetProxy() throws Exception {
    if (!canRunRandomDotOrgLargeTest()) {
      throw new SkipException("Test can't run on this platform");
    }
    RandomDotOrgSeedGenerator.setProxy(proxy);
    try {
      SeedTestUtils.testGenerator(seedGenerator);
    } finally {
      RandomDotOrgSeedGenerator.setProxy(null);
    }
  }
}
