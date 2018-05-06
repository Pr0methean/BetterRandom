package io.github.pr0methean.betterrandom.seed;

import static io.github.pr0methean.betterrandom.seed.RandomDotOrgSeedGenerator.setProxy;
import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.testng.Assert.assertSame;

import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@PrepareForTest(URL.class)
@Test(singleThreaded = true)
public class RandomDotOrgSeedGeneratorHermeticTest extends RandomDotOrgSeedGeneratorAbstractTest {
  private static final Charset UTF8 = Charset.forName("UTF-8");
  @SuppressWarnings({"HardcodedFileSeparator", "HardcodedLineSeparator"})
  private static final byte[] RESPONSE_32 = haveApiKey()
      ? ("{\"jsonrpc\":\"2.0\",\"result\":{\"random\":{\"data\":"
      + "[\"gAlhFSSjLy+u5P/Cz92BH4R3NZ0+j8UHNeIR02CChoQ=\"],"
      + "\"completionTime\":\"2018-05-06 19:54:31Z\"},\"bitsUsed\":256,\"bitsLeft\":996831,"
      + "\"requestsLeft\":199912,\"advisoryDelay\":290},\"id\":27341}").getBytes(UTF8)
      : ("19\ne0\ne9\n6b\n85\nbf\na5\n07\na7\ne9\n65\n2e\n90\n42\naa\n02\n2d\nc4\n67\n2a\na3\n32\n"
          + "9d\nbc\nd1\n9b\n2f\n7c\nf3\n60\n30\ne5").getBytes(UTF8);
  private URL mockUrl;
  private final URL realUrl = RandomDotOrgSeedGenerator.getJsonRequestUrl();
  private String[] address = {null};

  @BeforeClass
  @Override
  public void setUp() {
    super.setUp();
    mockUrl = mock(URL.class);
  }

  @Test
  public void testSetProxyHermetic() throws Exception {
    setProxy(proxy);
    try {
      enableMockUrl();
      final FakeHttpsUrlConnection[] connection = {null};
      when(mockUrl.openConnection(any(Proxy.class))).thenAnswer(invocationOnMock -> {
        assertSame(proxy, invocationOnMock.getArguments()[0]);
        connection[0] = new FakeHttpsUrlConnection(mockUrl, proxy, RESPONSE_32);
        return connection[0];
      });
      SeedTestUtils.testGenerator(seedGenerator);
    } finally {
      setProxy(null);
    }
  }

  @AfterMethod
  @Override
  public void tearDown() {
    super.tearDown();
    RandomDotOrgSeedGenerator.setJsonRequestUrl(realUrl);
  }

  private void enableMockUrl() throws Exception {
    PowerMockito.whenNew(URL.class).withAnyArguments().thenAnswer(invocationOnMock -> {
      address[0] = (String) (invocationOnMock.getArguments()[0]);
      return mockUrl;
    });
    RandomDotOrgSeedGenerator.setJsonRequestUrl(mockUrl);
  }
}
