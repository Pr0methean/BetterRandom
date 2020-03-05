package io.github.pr0methean.betterrandom.seed;

import static io.github.pr0methean.betterrandom.seed.SeedTestUtils.createSocketFactory;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import org.testng.SkipException;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

public abstract class WebJsonSeedGeneratorLiveTest<T extends WebJsonSeedGenerator>
    extends AbstractSeedGeneratorTest<T> {
  protected final Proxy proxy = SeedTestUtils.createProxy();

  public WebJsonSeedGeneratorLiveTest(T seedGenerator) {
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

  @Test public void testSetProxyReal() {
    try {
      new URL("https://google.com").openConnection(proxy).getContent();
    } catch (IOException e) {
      throw new SkipException("This test requires an HTTP proxy on localhost:8888");
    }
    seedGenerator.setProxy(proxy);
    try {
      SeedTestUtils.testGenerator(seedGenerator, true);
    } finally {
      seedGenerator.setProxy(null);
    }
  }

  @BeforeSuite public void setUpSuite() {
    seedGenerator.setSslSocketFactory(createSocketFactory()); // run all tests with POODLE protection
  }

  @AfterSuite public void tearDownSuite() {
    seedGenerator.setSslSocketFactory(null);
  }
}
