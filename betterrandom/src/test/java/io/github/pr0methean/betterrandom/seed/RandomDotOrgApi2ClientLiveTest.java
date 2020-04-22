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

import java.net.Proxy;
import java.time.Duration;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.net.ssl.SSLSocketFactory;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Unit test for the seed generator that connects to random.org to get seed data.
 *
 * @author Daniel Dyer
 * @author Chris Hennick
 */
@Test(singleThreaded = true) public class RandomDotOrgApi2ClientLiveTest
    extends WebSeedClientLiveTest<RandomDotOrgApi2Client> {

  @Override protected RandomDotOrgApi2Client getSeedGenerator(@Nullable Proxy proxy,
      @Nullable SSLSocketFactory socketFactory) {
    final String apiKeyString = System.getenv("RANDOM_DOT_ORG_KEY");
    if (apiKeyString == null) {
      throw new SkipException("This test can't run unless environment variable RANDOM_DOT_ORG_KEY is set");
    }
    return new RandomDotOrgApi2Client(new WebSeedClientConfiguration.Builder()
        .setProxy(proxy)
        .setSocketFactory(socketFactory)
        .setRetryDelay(Duration.ZERO)
        .build(),
        UUID.fromString(apiKeyString));
  }
}
