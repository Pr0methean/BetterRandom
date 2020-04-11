package io.github.pr0methean.betterrandom.seed;

import static io.github.pr0methean.betterrandom.seed.SeedTestUtils.createSocketFactory;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public abstract class WebSeedClientLiveTest<T extends WebSeedClient>
    extends SeedGeneratorTest<T> {
  protected final Proxy proxy = SeedTestUtils.createProxy();

  protected WebSeedClientLiveTest(T seedGenerator) {
    super(seedGenerator);
  }

  @Test public void testSetProxyOff() {
    seedGenerator.setProxy(Proxy.NO_PROXY);
    try {
      SeedTestUtils.testGenerator(seedGenerator, true);
    } finally {
      seedGenerator.setProxy(null);
    }
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
    seedGenerator.setProxy(proxy);
    try {
      testGenerator();
    } finally {
      seedGenerator.setProxy(null);
    }
  }

  @Override protected T getSeedGenerator() {
    T seedGenerator = super.getSeedGenerator();
    seedGenerator.setSslSocketFactory(createSocketFactory()); // run all tests with POODLE protection
    return seedGenerator;
  }

  @AfterMethod public void tearDownSuite() {
    seedGenerator.setSslSocketFactory(null);
  }
}
