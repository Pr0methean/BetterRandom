package io.github.pr0methean.betterrandom.seed;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

/**
 * Utility methods used in {@link RandomDotOrgSeedGeneratorHermeticTest} and
 * {@link RandomDotOrgSeedGeneratorLiveTest}.
 */
public enum RandomDotOrgUtils {
  ;

  private static final int TOR_PORT = 9050;

  public static boolean haveApiKey() {
    return System.getenv("RANDOM_DOT_ORG_KEY") != null;
  }

  public static void setApiKey() {
    final String apiKeyString = System.getenv("RANDOM_DOT_ORG_KEY");
    RandomDotOrgSeedGenerator
        .setApiKey((apiKeyString == null) ? null : UUID.fromString(apiKeyString));
  }

  public static Proxy createTorProxy() {
    return new Proxy(Type.SOCKS, new InetSocketAddress("localhost", TOR_PORT));
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
      throw new AssertionError(e);
    }
  }}
