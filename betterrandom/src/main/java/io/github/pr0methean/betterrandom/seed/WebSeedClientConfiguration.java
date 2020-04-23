package io.github.pr0methean.betterrandom.seed;

import java.io.Serializable;
import java.net.Proxy;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.net.ssl.SSLSocketFactory;

/**
 * Common configuration parameters for an instance of {@link WebSeedClient}. This class makes it
 * possible to add more parameters in the future without needing new constructor overloads in
 * {@link WebSeedClient} and all its subclasses.
 */
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
  protected WebSeedClientConfiguration(@Nullable Proxy proxy,
      @Nullable SSLSocketFactory socketFactory,
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
    @Nullable private Proxy proxy = null;
    @Nullable private SSLSocketFactory socketFactory = null;
    private long retryDelayMs = DEFAULT_RETRY_DELAY_MS;

    /**
     * Sets the proxy to use to connect to the server.
     *
     * @param proxy the proxy, or null (default) to use the system default
     * @return this builder
     */
    public Builder setProxy(@Nullable Proxy proxy) {
      this.proxy = proxy;
      return this;
    }

    /**
     * Sets the {@link SSLSocketFactory} used to connect to the server.
     *
     * @param socketFactory the socket factory, or null (default) to use the system default
     * @return this builder
     */
    public Builder setSocketFactory(SSLSocketFactory socketFactory) {
      this.socketFactory = socketFactory;
      return this;
    }

    /**
     * Sets the delay after an {@link java.io.IOException} before we will try connecting again. For
     * {@link RandomDotOrgApi2Client}, this also controls the maximum "advisory delay" that will be
     * honored. Attempting to use the {@link WebSeedClient} before the delay ends will throw a
     * {@link SeedException}, so this is best used in a {@link SeedGeneratorPreferenceList}.
     *
     * @param delay the delay after a failed request
     * @return this builder
     */
    public Builder setRetryDelay(Duration delay) {
      this.retryDelayMs = delay.toMillis();
      return this;
    }

    /**
     * Sets the delay after an {@link java.io.IOException} before we will try connecting again. For
     * {@link RandomDotOrgApi2Client}, this also controls the maximum "advisory delay" that will be
     * honored. Attempting to use the {@link WebSeedClient} before the delay ends will throw a
     * {@link SeedException}, so this is best used in a {@link SeedGeneratorPreferenceList}.
     *
     * @param amount the number of units to delay by
     * @param unit the time unit in which the delay is measured
     * @return this builder
     */
    public Builder setRetryDelay(long amount, TimeUnit unit) {
      this.retryDelayMs = unit.toMillis(amount);
      return this;
    }

    /**
     * Builds a {@link WebSeedClientConfiguration} with this builder's parameters.
     * @return a {@link WebSeedClientConfiguration}
     */
    public WebSeedClientConfiguration build() {
      return new WebSeedClientConfiguration(proxy, socketFactory, retryDelayMs);
    }

  }
}
