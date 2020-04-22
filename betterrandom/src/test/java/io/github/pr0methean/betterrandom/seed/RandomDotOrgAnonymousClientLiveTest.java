package io.github.pr0methean.betterrandom.seed;

import java.net.Proxy;
import javax.annotation.Nullable;
import javax.net.ssl.SSLSocketFactory;

public class RandomDotOrgAnonymousClientLiveTest extends WebSeedClientLiveTest<RandomDotOrgAnonymousClient> {

  @Override protected RandomDotOrgAnonymousClient getSeedGenerator(@Nullable Proxy proxy,
      @Nullable SSLSocketFactory socketFactory) {
    return new RandomDotOrgAnonymousClient(new WebSeedClientConfiguration.Builder()
        .setProxy(proxy)
        .setSocketFactory(socketFactory)
        .build());
  }
}
