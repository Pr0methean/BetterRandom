package io.github.pr0methean.betterrandom.seed;

import static io.github.pr0methean.betterrandom.seed.SeedTestUtils.createSocketFactory;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import javax.annotation.Nullable;
import javax.net.ssl.SSLSocketFactory;
import org.testng.SkipException;
import org.testng.annotations.Test;

public abstract class WebSeedClientLiveTest<T extends WebSeedClient>
    extends SeedGeneratorTest<T> {
  protected final Proxy proxy = SeedTestUtils.createProxy();

  protected WebSeedClientLiveTest() {
    super();
  }

  @Test public void testSetProxyOff() {
    SeedTestUtils.testGenerator(getSeedGenerator(Proxy.NO_PROXY, null), true);
  }

  @Test public void testGenerator() {
    SeedTestUtils.testGenerator(seedGenerator, true);
  }

  @Test public void testSetProxyReal() {
    try {
      new URL("https://google.com").openConnection(proxy).getContent();
    } catch (IOException e) {
      throw new SkipException("This test requires an HTTP proxy on localhost:8888");
    }
    SeedTestUtils.testGenerator(getSeedGenerator(proxy, null), true);
  }

  @Override protected T initializeSeedGenerator() {
    return getSeedGenerator(null, createSocketFactory());
  }

  protected abstract T getSeedGenerator(@Nullable Proxy proxy, @Nullable SSLSocketFactory socketFactory);
}
