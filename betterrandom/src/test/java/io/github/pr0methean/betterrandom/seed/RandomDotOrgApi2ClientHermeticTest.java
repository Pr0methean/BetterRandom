package io.github.pr0methean.betterrandom.seed;

import static io.github.pr0methean.betterrandom.TestUtils.fail;
import static io.github.pr0methean.betterrandom.seed.SeedTestUtils.testGenerator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import io.github.pr0methean.betterrandom.util.BinaryUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Proxy;
import java.util.Base64;
import java.util.UUID;
import javax.net.ssl.SSLSocketFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.mockito.mockpolicies.Slf4jMockPolicy;
import org.powermock.core.classloader.annotations.MockPolicy;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.nashorn.*", "javax.net.ssl.*",
    "javax.security.*"}) @MockPolicy(Slf4jMockPolicy.class)
@PrepareForTest(RandomDotOrgApi2Client.class) @Test(singleThreaded = true)
@SuppressWarnings("ThrowableNotThrown")
public class RandomDotOrgApi2ClientHermeticTest
    extends WebSeedClientHermeticTest<RandomDotOrgApi2Client> {

  @SuppressWarnings("HardcodedFileSeparator") public static final byte[] RESPONSE_32 =
      ("{\"jsonrpc\":\"2.0\",\"result\":{\"random\":{\"data\":" +
          "[\"gAlhFSSjLy+u5P/Cz92BH4R3NZ0+j8UHNeIR02CChoQ=\"]," +
          "\"completionTime\":\"2018-05-06 19:54:31Z\"},\"bitsUsed\":256,\"bitsLeft\":996831," +
          "\"requestsLeft\":199912,\"advisoryDelay\":290},\"id\":27341}").getBytes(UTF_8);
  @SuppressWarnings(
      {"HardcodedFileSeparator", "SpellCheckingInspection"}) public static final byte[]
      RESPONSE_625 = ("{\"jsonrpc\":\"2.0\",\"result\":{\"random\":{\"data\":" +
      "[\"Tcx+z1Q4AN4j9IGSNkAPl38/Tfh16WW+1p6Gbbss/wenciNlyaBneI8PyHNB5m3/oKj9M9F+PEbF" +
      "uPNsupWjx5YIHxkSlFJo7emuQJ0NLScDBT+mMKLc58FwEpu0i+tklbm3pXctSuZvJ68In7HZGe29" +
      "5rhwXdRiB4JCEkE214RQlS3bSYGnxGODjvHxiwwR80VLTLUZe6sFlpeY1fcuzn3K+fmO2eVyMdKe" +
      "gL/74nTe6u9R6hGQ7AehX8NP7dEKhUaTVv43NGcXCOX6rdBzo6U82CsQKkNHXa8FhJLDOSlFENhH" +
      "B6eI/WZfImtbxFLFSr6PI8A92472kK1bDqTOlvRPQAjLc1T1yuEW6tsA9z3Wfm0YozojtUYc0xED" +
      "XP2rf89oHsK5wni4w1qkY/VYAdjWv2vZjSXNZ/c/qRli7pk7/hG3oXPoZ5GpLfuv7k+hRgD7Zcr+" +
      "JNbLSlSUtGQtNmK18s/mhrvmmE3brk/wGpdqaBeOs6SFmBZf1iSAFzn9RPjhWfZIBsUY0YSb8RWy" +
      "oBV7acIfXzKRUyE7cfqSGnrsIqcr7FWHXe3kodOF0nzfOsZGzx+sL0/GsRzW/WahlxtleQyatmyP" +
      "/JJ/3c5UiLVY58+hkJdokqtDGe/2F1itMAGPOiCcfUc+O1dtf9YEgLmaYpEupkAvgaGop2/qDh+i" +
      "XcH3ShjCQVsVOmTzw6AharEQYcz8sML+pu12LusAJc61sKZ9TamddrpKljmH2liB6GFs8DD7DyFB" +
      "V/7ORy6SWbejQd2wQ2fz2UAJ1aZME/ODGgYCWXPQOTMNcl+eaF2ubUf7BI2G0w+xP7tbum7GRQ==\"]," +
      "\"completionTime\":\"2018-05-06 19:54:31Z\"},\"bitsUsed\":256,\"bitsLeft\":996831," +
      "\"requestsLeft\":199912,\"advisoryDelay\":290},\"id\":27341}").getBytes(UTF_8);

  private final byte[] MAX_SIZE_RESPONSE_NEW_API;

  {
    try {
      ByteArrayOutputStream responseBuilder = new ByteArrayOutputStream();
      PrintStream stream = new PrintStream(responseBuilder, false, "UTF-8");
      stream
          .print("{\"jsonrpc\":\"2.0\",\"result\":{\"random\":{\"data\":[\"");
      stream.write(Base64.getEncoder().encode(expectedHugeSeed));
      stream.print("\"]," +
          "\"completionTime\":\"2018-05-06 19:54:31Z\"},\"bitsUsed\":256,\"bitsLeft\":996831," +
          "\"requestsLeft\":199912,\"advisoryDelay\":290},\"id\":27341}");
      MAX_SIZE_RESPONSE_NEW_API = responseBuilder.toByteArray();
    } catch (IOException e) {
      throw fail("Error setting up test class", e);
    }
  }

  @BeforeMethod public void setUpMethod() {
    seedGenerator = PowerMockito.spy(getSeedGenerator());
  }

  @AfterMethod public void tearDownMethod() {
    address = null;
  }

  @Test public void testSetProxy() {
    seedGenerator = getSeedGenerator(proxy, null);
    mockResponse(RESPONSE_625);
    testGenerator(seedGenerator, false);
    assertNotNull(address);
    assertTrue(address.startsWith("https://api.random.org/json-rpc/2/invoke"));
  }

  @Test public void testOverLongResponse() {
    mockResponse(RESPONSE_625);
    testGenerator(seedGenerator, false);
  }

  @Test public void testOverShortResponse() {
    mockResponse(RESPONSE_32);
    expectAndGetException(625, false);
  }

  @Test public void testInvalidBase64Response() {
    mockResponse("{\"jsonrpc\":\"2.0\",\"result\":{\"random\":{\"data\":" +
        "[\"\uD83D\uDCA9lhFSSjLy+u5P/Cz92BH4R3NZ0+j8UHNeIR02CChoQ=\"]," +
        "\"completionTime\":\"2018-05-06 19:54:31Z\"},\"bitsUsed\":256,\"bitsLeft\":996831," +
        "\"requestsLeft\":199912,\"advisoryDelay\":290},\"id\":27341}");
    expectAndGetException(32);
  }

  @Test public void testResponseError() {
    mockResponse("{\"jsonrpc\":\"2.0\",\"error\":\"Oh noes, an error\"," +
          "\"result\":{\"random\":{\"data\":" +
          "[\"gAlhFSSjLy+u5P/Cz92BH4R3NZ0+j8UHNeIR02CChoQ=\"]," +
          "\"completionTime\":\"2018-05-06 19:54:31Z\"},\"bitsUsed\":256,\"bitsLeft\":996831," +
          "\"requestsLeft\":199912,\"advisoryDelay\":290},\"id\":27341}");
    assertEquals(
        expectAndGetException(SeedTestUtils.SEED_SIZE, false).getMessage(), "Oh noes, an error",
        "Wrong exception message");
  }

  @Test public void testResponseNoResult() {
    mockResponse("{\"jsonrpc\":\"2.0\"}");
    expectAndGetException(SeedTestUtils.SEED_SIZE, false);
  }

  @Test public void testResponseNoRandom() {
    mockResponse("{\"jsonrpc\":\"2.0\",\"result\":{" +
        "\"completionTime\":\"2018-05-06 19:54:31Z\"},\"bitsUsed\":256,\"bitsLeft\":996831," +
        "\"requestsLeft\":199912,\"advisoryDelay\":290},\"id\":27341}");
    expectAndGetException(SeedTestUtils.SEED_SIZE);
  }

  /**
   * Try to acquire a large number of bytes, more than are cached internally by the seed generator
   * implementation.
   */
  @Test(timeOut = 120000) public void testLargeRequest() {
    // Request more bytes than can be gotten in one Web request.
    mockResponse(MAX_SIZE_RESPONSE_NEW_API);
    tryLargeRequest();
  }

  @Test public void testRandomFuzz() {
    // invocationCount spams the log, so use a loop
    for (int i = 0; i < 10_000; i++) {
      String fuzzOut = BinaryUtils.convertBytesToHexString(fuzzResponse(RESPONSE_32.length));
      expectAndGetException(32, false, fuzzOut);
    }
  }

  @Override protected byte[] get32ByteResponse() {
    return RESPONSE_32;
  }

  @Test public void testNullApiKey() {
    try {
      new RandomDotOrgApi2Client(true, null);
      fail("Instantiating with apiKey null should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException expected) {}
  }

  @Override public void testToString() {
    super.testToString();
    Assert.assertNotNull(new RandomDotOrgApi2Client(false, UUID.randomUUID()).toString());
  }

  @Override protected RandomDotOrgApi2Client getSeedGenerator(Proxy proxy,
      SSLSocketFactory socketFactory) {
    return new RandomDotOrgApi2Client(true, UUID.randomUUID());
  }
}
