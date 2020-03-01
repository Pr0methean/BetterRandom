package io.github.pr0methean.betterrandom.seed;

import static io.github.pr0methean.betterrandom.seed.RandomDotOrgUtils.createProxy;
import static org.mockito.ArgumentMatchers.any;

import java.net.Proxy;
import java.net.URL;
import javax.annotation.Nullable;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;

public abstract class AbstractWebJsonSeedGeneratorHermeticTest<T extends SeedGenerator>
    extends PowerMockTestCase {
  protected final Proxy proxy = createProxy();
  protected T seedGenerator;
  @Nullable protected String address = null;

  protected void mockResponse(final byte[] response) {
    seedGenerator = PowerMockito.spy(getSeedGeneratorUnderTest());
    try {
      PowerMockito.doAnswer(invocationOnMock -> {
        final URL url = invocationOnMock.getArgument(0);
        address = url.toString();
        return new FakeHttpsUrlConnection(url, null, response);
      }).when(seedGenerator, "openConnection", any(URL.class));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected SeedException expectAndGetException(int seedSize) {
    SeedException exception = null;
    try {
      seedGenerator.generateSeed(seedSize);
      Assert.fail("Should have thrown SeedException");
    } catch (final SeedException expected) {
      exception = expected;
    }
    return exception;
  }

  protected abstract T getSeedGeneratorUnderTest();
}
