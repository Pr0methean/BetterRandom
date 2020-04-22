package io.github.pr0methean.betterrandom.seed;

import java.io.Serializable;
import java.net.Proxy;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.net.ssl.SSLSocketFactory;

public class WebSeedClientConfiguration implements Serializable {
  private static final long serialVersionUID = -5162594092589771385L;
  public static final long DEFAULT_RETRY_DELAY_MS = 10000;
  /**
   * Default configuration.
   */
  public static final WebSeedClientConfiguration DEFAULT = new Builder().build();

  @Nullable private final transient Proxy proxy;
  @Nullable private final transient SSLSocketFactory socketFactory;
  private final long retryDelayMs;

  /**
   * @param proxy the proxy to use with this server, or null to use the JVM default
   * @param socketFactory the socket factory, or null for the JVM default
   * @param retryDelayMs time to wait before trying again after an IOException
   */
  public WebSeedClientConfiguration(@Nullable Proxy proxy, @Nullable SSLSocketFactory socketFactory,
      long retryDelayMs) {
    this.proxy = proxy;
    this.socketFactory = socketFactory;
    this.retryDelayMs = retryDelayMs;
  }

  @Nullable public Proxy getProxy() {
    return proxy;
  }

  @Nullable public SSLSocketFactory getSocketFactory() {
    return socketFactory;
  }

  public long getRetryDelayMs() {
    return retryDelayMs;
  }

  @Override public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WebSeedClientConfiguration that = (WebSeedClientConfiguration) o;
    return retryDelayMs == that.retryDelayMs && Objects.equals(proxy, that.proxy) &&
        Objects.equals(socketFactory, that.socketFactory);
  }

  @Override public int hashCode() {
    return Objects.hash(proxy, socketFactory, retryDelayMs);
  }

  public static class Builder {
    private Proxy proxy = null;
    private SSLSocketFactory socketFactory = null;
    private long retryDelayMs = DEFAULT_RETRY_DELAY_MS;

    public Builder setProxy(Proxy proxy) {
      this.proxy = proxy;
      return this;
    }

    public Builder setSocketFactory(SSLSocketFactory socketFactory) {
      this.socketFactory = socketFactory;
      return this;
    }

    public Builder setRetryDelay(Duration delay) {
      this.retryDelayMs = delay.toMillis();
      return this;
    }

    public Builder setRetryDelay(long amount, TimeUnit unit) {
      this.retryDelayMs = unit.toMillis(amount);
      return this;
    }

    public WebSeedClientConfiguration build() {
      return new WebSeedClientConfiguration(proxy, socketFactory, retryDelayMs);
    }

    public Builder setRetryDelayMs(long retryDelayMs) {
      this.retryDelayMs = retryDelayMs;
      return this;
    }
  }
}
