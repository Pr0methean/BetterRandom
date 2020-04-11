package io.github.pr0methean.betterrandom.seed;

import static io.github.pr0methean.betterrandom.seed.SeedTestUtils.createProxy;
import static io.github.pr0methean.betterrandom.seed.SeedTestUtils.createSocketFactory;
import static io.github.pr0methean.betterrandom.seed.SeedTestUtils.testGenerator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.any;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import io.github.pr0methean.betterrandom.TestUtils;
import java.net.Proxy;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import org.json.simple.parser.ParseException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

@SuppressWarnings("ThrowableNotThrown")
public abstract class WebSeedClientHermeticTest<T extends WebSeedClient>
    extends SeedGeneratorTest<T> {
  protected final int maxRequestSize
      = getSeedGenerator().getMaxRequestSize();
  protected final byte[] expectedHugeSeed = new byte[maxRequestSize + 1];

  {
    ThreadLocalRandom.current().nextBytes(expectedHugeSeed);
    // First byte must equal last, since request will be made twice
    expectedHugeSeed[maxRequestSize] = expectedHugeSeed[0];
  }
  protected final Proxy proxy = createProxy();
  protected T seedGenerator;
  @Nullable protected volatile String address = null;

  protected WebSeedClientHermeticTest() {
    super(null);
  }

  @AfterMethod public void tearDown() {
    address = null;
  }

  protected void mockResponse(String response) {
    mockResponse(response.getBytes(UTF_8));
  }

  protected void mockResponse(final byte[] response) {
    // For some reason, fuzz tests run slower if we track whether seedGenerator is already a spy
    seedGenerator = PowerMockito.spy(getSeedGenerator());
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
    seedGenerator = getSeedGenerator();
    seedGenerator.setSslSocketFactory(createSocketFactory());
    try {
      testBasicResponse();
    } finally {
      seedGenerator.setSslSocketFactory(null);
    }
  }

  @Test public void testBasicResponse() {
    mockResponse(get32ByteResponse());
    testGenerator(seedGenerator, false, 32);
  }

  protected abstract byte[] get32ByteResponse();

  protected abstract T getSeedGenerator();

  protected void tryLargeRequest() {
    final int seedLength = seedGenerator.getMaxRequestSize() + 1;
    byte[] seed = seedGenerator.generateSeed(seedLength);
    Assert.assertEquals(seed.length, seedLength, "Failed to generate seed of length " + seedLength);
    assertTrue(Arrays.equals(seed, expectedHugeSeed), "Seed output not as expected");
  }

  @Test public void testEmptyResponse() {
    mockResponse("");
    expectAndGetException(SeedTestUtils.SEED_SIZE, false);
  }

  @Test public void testNonJsonResponse() {
    mockResponse("Not JSON");
    expectAndGetException(SeedTestUtils.SEED_SIZE, true);
  }

  @Test public void testNumericResponse() {
    mockResponse("123456789");
    expectAndGetException(SeedTestUtils.SEED_SIZE, false);
  }

  @Test public void testSerializable() {
    TestUtils.assertEqualAfterSerialization(getSeedGenerator());
  }

  @Test public void testToString() {
  }
}
