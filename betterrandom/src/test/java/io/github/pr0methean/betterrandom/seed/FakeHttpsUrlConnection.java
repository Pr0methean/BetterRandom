package io.github.pr0methean.betterrandom.seed;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Proxy;
import java.net.URL;
import java.security.cert.Certificate;
import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;

/**
 * Used for testing {@link RandomDotOrgSeedGenerator}.
 */
public class FakeHttpsUrlConnection extends HttpsURLConnection {

  private static final Certificate[] CERTIFICATES = new Certificate[0];
  @Nullable private final Proxy proxy;
  private volatile boolean disconnected = false;
  private final ByteArrayOutputStream os = new ByteArrayOutputStream();
  private final ByteArrayInputStream is;

  public FakeHttpsUrlConnection(final URL url, @Nullable final Proxy proxy,
      final byte[] responseBody) {
    super(url);
    this.proxy = proxy;
    is = new ByteArrayInputStream(responseBody);
  }

  @Override public InputStream getInputStream() {
    if (disconnected) {
      throw new IllegalStateException("Already disconnected");
    }
    return is;
  }

  @Override public OutputStream getOutputStream() {
    return os;
  }

  @Override public String getCipherSuite() {
    return null;
  }

  @Override public Certificate[] getLocalCertificates() {
    return CERTIFICATES;
  }

  @Override public Certificate[] getServerCertificates() {
    return CERTIFICATES;
  }

  @Override public void disconnect() {
    disconnected = true;
  }

  @Override public boolean usingProxy() {
    return (proxy != null) && !proxy.equals(Proxy.NO_PROXY);
  }

  @Override public void connect() {
    connected = true;
  }

  @Override protected void finalize() {
    try {
      if (is != null) {
        is.close();
      }
      os.close();
    } catch (final IOException ignored) {}
  }
}
