package io.github.pr0methean.betterrandom.seed;

import static io.github.pr0methean.betterrandom.seed.SeedTestUtils.createProxy;
import static io.github.pr0methean.betterrandom.seed.SeedTestUtils.createSocketFactory;
import static io.github.pr0methean.betterrandom.seed.SeedTestUtils.testGenerator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.any;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.net.Proxy;
import java.net.URL;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import org.json.simple.parser.ParseException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

@SuppressWarnings("ThrowableNotThrown")
public abstract class WebJsonSeedGeneratorHermeticTest<T extends WebJsonSeedGenerator>
    extends PowerMockTestCase {
  protected final Proxy proxy = createProxy();
  protected T seedGenerator;
  @Nullable protected volatile String address = null;

  @AfterMethod public void tearDown() {
    address = null;
  }

  protected void mockResponse(String response) {
    mockResponse(response.getBytes(UTF_8));
  }

  protected void mockResponse(final byte[] response) {
    seedGenerator = PowerMockito.spy(getSeedGeneratorUnderTest());
    try {
      PowerMockito.doAnswer(invocationOnMock -> {
        final URL url = invocationOnMock.getArgument(0);
        address = url.toString();
        return new FakeHttpsUrlConnection(url, seedGenerator.proxy.get(), response);
      }).when(seedGenerator, "openConnection", any(URL.class));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected SeedException expectAndGetException(int seedSize) {
    return expectAndGetException(seedSize, true);
  }

  protected SeedException expectAndGetException(int seedSize, boolean expectCause) {
    return expectAndGetException(seedSize, expectCause, null);
  }

  protected SeedException expectAndGetException(int seedSize, boolean expectCause,
      @Nullable String message) {
    SeedException exception = null;
    try {
      seedGenerator.generateSeed(seedSize);
      fail(message == null ? "Should have thrown SeedException" : message);
    } catch (final SeedException expected) {
      exception = expected;
    }
    if (expectCause && exception.getCause() == null) {
      fail("SeedException should have a cause", exception);
    }
    return exception;
  }

  protected byte[] fuzzResponse(int length) {
    byte[] fuzz = new byte[length];
    ThreadLocalRandom.current().nextBytes(fuzz);
    mockResponse(fuzz);
    return fuzz;
  }

  @Test public void testSetSslSocketFactory() {
    seedGenerator = getSeedGeneratorUnderTest();
    seedGenerator.setSslSocketFactory(createSocketFactory());
    try {
      testGenerator(seedGenerator, false);
    } finally {
      seedGenerator.setSslSocketFactory(null);
    }
  }

  protected abstract T getSeedGeneratorUnderTest();

  @Test public void testEmptyResponse() {
    mockResponse("");
    expectAndGetException(SeedTestUtils.SEED_SIZE, false);
  }

  @Test public void testNonJsonResponse() {
    mockResponse("Not JSON");
    assertTrue(
        expectAndGetException(SeedTestUtils.SEED_SIZE).getCause() instanceof ParseException,
        "Wrong type of exception cause");
  }

  @Test public void testNumericResponse() {
    mockResponse("123456789");
    expectAndGetException(SeedTestUtils.SEED_SIZE, false);
  }
}
