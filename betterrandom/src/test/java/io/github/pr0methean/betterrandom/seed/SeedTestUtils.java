package io.github.pr0methean.betterrandom.seed;

import static io.github.pr0methean.betterrandom.TestUtils.fail;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

enum SeedTestUtils {
  ;

  public static final int SEED_SIZE = 16;
  @SuppressWarnings("MismatchedReadAndWriteOfArray") private static final byte[] ALL_ZEROES =
      new byte[SEED_SIZE];
  private static final int PROXY_PORT = 8888;

  public static void testGenerator(final SeedGenerator seedGenerator, boolean expectNonIdempotent) {
    testGenerator(seedGenerator, expectNonIdempotent, SEED_SIZE);
  }

  public static void testGenerator(final SeedGenerator seedGenerator, boolean expectNonIdempotent,
      int seedSize) {
    final byte[] seed = seedGenerator.generateSeed(seedSize);
    assertEquals(seed.length, seedSize, "Failed to generate seed of correct length");
    assertFalse(Arrays.equals(seed, ALL_ZEROES), "Generated an all-zeroes seed");
    if (expectNonIdempotent) {
      final byte[] secondSeed = new byte[seedSize];
      seedGenerator.generateSeed(secondSeed); // Check that other syntax also works
      assertFalse(Arrays.equals(secondSeed, ALL_ZEROES));
      assertFalse(Arrays.equals(seed, secondSeed));
    }
  }

  public static Proxy createProxy() {
    return new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", PROXY_PORT));
  }

  public static SSLSocketFactory createSocketFactory() {
    try {
      String[] versionNumberParts = System.getProperty("java.version").split("\\.");
      int javaVersion = Integer.parseInt(versionNumberParts[0]);
      if (javaVersion == 1) {
        javaVersion = Integer.parseInt(versionNumberParts[1]);
      }
      System.out.println("Detected JDK version " + javaVersion);
      SSLContext context = SSLContext.getInstance(javaVersion >= 11 ? "TLSv1.3" : "TLSv1.2");
      context.init(null, null, null);
      return context.getSocketFactory();
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      throw fail("Unable to set up SSLSocketFactory", e);
    }
  }
}
