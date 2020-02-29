package io.github.pr0methean.betterrandom.seed;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import org.json.simple.JSONObject;

public abstract class WebJsonSeedGenerator implements SeedGenerator {
  /**
   * Measures the retry delay. A ten-second delay might become either nothing or an hour if we used
   * local time during the start or end of Daylight Saving Time, but it's fine if we occasionally
   * wait 9 or 11 seconds instead of 10 because of a leap-second adjustment. See <a
   * href="https://www.youtube.com/watch?v=-5wpm-gesOY">Tom Scott's video</a> about the various
   * considerations involved in this choice of clock.
   */
  protected static final Clock CLOCK = Clock.systemUTC();
  private static final int RETRY_DELAY_MS = 10000;
  private static final Duration RETRY_DELAY = Duration.ofMillis(RETRY_DELAY_MS);
  protected static final Lock lock = new ReentrantLock();
  protected static volatile Instant earliestNextAttempt = Instant.MIN;
  /**
   * The proxy to use with this server, or null to use the JVM default.
   */
  protected final AtomicReference<Proxy> proxy = new AtomicReference<>(null);
  /**
   * The SSLSocketFactory to use with this server.
   */
  protected final AtomicReference<SSLSocketFactory> socketFactory =
      new AtomicReference<>(null);
  /**
   * If true, don't attempt to contact the server again for RETRY_DELAY after an IOException
   */
  protected final boolean useRetryDelay;

  public WebJsonSeedGenerator(final boolean useRetryDelay) {
    this.useRetryDelay = useRetryDelay;
  }

  protected static <T> T checkedGetObject(final JSONObject parent, final String key,
      Class<T> outputClass) {
    Object child = parent.get(key);
    if (!outputClass.isInstance(child)) {
      throw new SeedException(String.format("Expected %s to have child key %s of type %s",
          parent, key, outputClass));
    }
    return outputClass.cast(child);
  }

  public Duration getRetryDelay() {
    return RETRY_DELAY;
  }

  public int getRetryDelayMs() {
    return RETRY_DELAY_MS;
  }

  protected abstract String getUserAgent();

  protected abstract int getMaxRequestSize();

  /**
   * Sets the proxy to use to connect to random.org. If null, the JVM default is used.
   *
   * @param proxy a proxy, or null for the JVM default
   */
  public void setProxy(@Nullable final Proxy proxy) {
    this.proxy.set(proxy);
  }

  /**
   * Sets the socket factory to use to connect to random.org. If null, the JVM default is used. This
   * method provides flexibility in how the user protects against downgrade attacks such as POODLE
   * and weak cipher suites, even if the random.org connection needs separate handling from
   * connections to other services by the same application.
   *
   * @param socketFactory a socket factory, or null for the JVM default
   */
  public void setSslSocketFactory(@Nullable final SSLSocketFactory socketFactory) {
    this.socketFactory.set(socketFactory);
  }

  protected HttpURLConnection openConnection(final URL url) throws IOException {
    final Proxy currentProxy = proxy.get();
    final HttpsURLConnection connection =
        (HttpsURLConnection) ((currentProxy == null) ? url.openConnection() :
            url.openConnection(currentProxy));
    final SSLSocketFactory currentSocketFactory = socketFactory.get();
    if (currentSocketFactory != null) {
      connection.setSSLSocketFactory(currentSocketFactory);
    }
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setRequestProperty("User-Agent", getUserAgent());
    return connection;
  }

  protected abstract void downloadBytes(byte[] seed, int offset, int length) throws IOException;

  @Override @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
  public void generateSeed(final byte[] seed) throws SeedException {
    if (!isWorthTrying()) {
      throw new SeedException("Not retrying so soon after an IOException");
    }
    final int length = seed.length;
    lock.lock();
    try {
      int count = 0;
      while (count < length) {
        int batchSize = Math.min(length - count, getMaxRequestSize());
        downloadBytes(seed, count, batchSize);
        count += batchSize;
      }
    } catch (final IOException ex) {
      earliestNextAttempt = CLOCK.instant().plus(getRetryDelay());
      throw new SeedException("Failed downloading bytes", ex);
    } catch (final SecurityException ex) {
      // Might be thrown if resource access is restricted (such as in an applet sandbox).
      throw new SeedException("SecurityManager prevented access to a remote seed source", ex);
    } finally {
      lock.unlock();
    }
  }

  @Override public boolean isWorthTrying() {
    return !useRetryDelay || !earliestNextAttempt.isAfter(RandomDotOrgSeedGenerator.CLOCK.instant());
  }
}
