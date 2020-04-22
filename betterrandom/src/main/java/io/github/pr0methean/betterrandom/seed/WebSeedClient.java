package io.github.pr0methean.betterrandom.seed;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * A {@link SeedGenerator} that is a client for a Web random-number service. Contains many methods
 * for parsing JSON responses.
 */
public abstract class WebSeedClient implements SeedGenerator {
  /**
   * Measures the retry delay. A ten-second delay might become either nothing or an hour if we used
   * local time during the start or end of Daylight Saving Time, but it's fine if we occasionally
   * wait 9 or 11 seconds instead of 10 because of a leap-second adjustment. See <a
   * href="https://www.youtube.com/watch?v=-5wpm-gesOY">Tom Scott's video</a> about the various
   * considerations involved in this choice of clock.
   */
  protected static final Clock CLOCK = Clock.systemUTC();
  /**
   * Made available to parse JSON responses.
   */
  protected static final JSONParser JSON_PARSER = new JSONParser();
  private static final long serialVersionUID = 2216766353219231461L;
  /**
   * Held while downloading, so that two requests to the same server won't be pending at the same
   * time.
   */
  protected final Lock lock = new ReentrantLock(true);
  /**
   * The earliest time we'll try again if there's been a previous IOE, or when the server requests
   * throttling.
   */
  protected volatile Instant earliestNextAttempt = Instant.MIN;
  private final WebSeedClientConfiguration configuration;

  /**
   * The value for the HTTP User-Agent header.
   */
  protected final String userAgent;

  /**
   * @param proxy the proxy to use with this server, or null to use the JVM default
   * @param socketFactory the socket factory, or null for the JVM default
   * @param useRetryDelay whether to wait 10 seconds before trying again after an IOException
   * @deprecated Use one of the other overloads, which allow specifying the delay before retry.
   */
  @Deprecated
  protected WebSeedClient(@Nullable final Proxy proxy,
      @Nullable final SSLSocketFactory socketFactory, final boolean useRetryDelay) {
    this(new WebSeedClientConfiguration.Builder().setProxy(proxy).setSocketFactory(socketFactory)
        .setRetryDelay(useRetryDelay
            ? Duration.ofMillis(WebSeedClientConfiguration.DEFAULT_RETRY_DELAY_MS)
            : Duration.ZERO)
        .build());
  }

  /**
   * @param webSeedClientConfiguration configuration
   */
  protected WebSeedClient(WebSeedClientConfiguration webSeedClientConfiguration) {
    configuration = webSeedClientConfiguration;
    userAgent = getClass().getName();
  }

  protected WebSeedClient() {
    this(WebSeedClientConfiguration.DEFAULT);
  }

  /**
   * Reads a field value from a JSON object and checks that it is the correct type.
   * @param parent the JSON object to retrieve a field from
   * @param key the field name
   * @param outputClass the object class that we expect the value to be
   * @param <T> the type of {@code outputClass}
   * @return the field value
   * @throws SeedException if the field is missing or the wrong type
   */
  protected static <T> T checkedGetObject(final JSONObject parent, final String key,
      Class<T> outputClass) {
    Object child = parent.get(key);
    if (!outputClass.isInstance(child)) {
      throw new SeedException(String.format("Expected %s to have child key %s of type %s",
          parent, key, outputClass));
    }
    return outputClass.cast(child);
  }

  /**
   * Creates a {@link BufferedReader} reading the response from the given {@link HttpURLConnection}
   * as UTF-8. The connection must be open and all request properties must be set before this reader
   * is used.
   *
   * @param connection the connection to read the response from
   * @return a BufferedReader reading the response
   * @throws IOException if thrown by {@link HttpURLConnection#getInputStream()}
   */
  protected static BufferedReader getResponseReader(final HttpURLConnection connection)
      throws IOException {
    return new BufferedReader(new InputStreamReader(connection.getInputStream(), UTF_8));
  }

  /**
   * Parses the response from the given {@link HttpURLConnection} as UTF-8 encoded JSON.
   *
   * @param connection the connection to parse the response from
   * @return the response as a {@link JSONObject}
   * @throws IOException if thrown by {@link HttpURLConnection#getInputStream()}
   */
  protected static JSONObject parseJsonResponse(HttpURLConnection connection) throws IOException {
    final Object response;
    try (final BufferedReader reader = getResponseReader(connection)) {
      response = JSON_PARSER.parse(reader);
    } catch (final ParseException e) {
      throw new SeedException("Unparseable JSON response", e);
    }
    if (!(response instanceof JSONObject)) {
      throw new SeedException(String.format("Response %s is not a JSON object", response));
    }
    return (JSONObject) response;
  }

  /**
   * Returns the maximum number of bytes that can be obtained with one request to the service.
   * When a seed larger than this is needed, it is obtained using multiple requests.
   *
   * @return the maximum number of bytes per request
   */
  protected abstract int getMaxRequestSize();

  /**
   * Opens an {@link HttpsURLConnection} that will make a GET request to the given URL using this
   * seed generator's current {@link Proxy}, {@link SeedGenerator} and User-Agent string, with the
   * header {@code Content-Type: application/json}.
   *
   * @param url the URL to connect to
   * @return a connection to the URL
   * @throws IOException if thrown by {@link URL#openConnection()} or {@link URL#openConnection(Proxy)}
   */
  protected HttpsURLConnection openConnection(final URL url) throws IOException {
    final HttpsURLConnection connection =
        (HttpsURLConnection) ((getProxy() == null) ? url.openConnection() :
            url.openConnection(getProxy()));
    if (getSocketFactory() != null) {
      connection.setSSLSocketFactory(getSocketFactory());
    }
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setRequestProperty("User-Agent", userAgent);
    return connection;
  }

  protected abstract URL getConnectionUrl(int numBytes);

  /**
   * Performs a single request for random bytes.
   *
   * @param connection the connection to download from
   * @param seed the array to save them to
   * @param offset the first index to save them to in the array
   * @param length the number of bytes to download
   * @throws IOException if a connection error occurs
   * @throws SeedException if a malformed response is received
   */
  protected abstract void downloadBytes(HttpURLConnection connection, byte[] seed, int offset,
      int length) throws IOException;

  @Override public void generateSeed(final byte[] seed) throws SeedException {
    if (!isWorthTrying()) {
      throw new SeedException("Not using this again until " + earliestNextAttempt);
    }
    final int length = seed.length;
    final int batchSize = Math.min(length, getMaxRequestSize());
    final URL batchUrl = getConnectionUrl(batchSize);
    final int batches = divideRoundingUp(length, batchSize);
    final int lastBatchSize = modRange1ToM(length, batchSize);
    final URL lastBatchUrl = getConnectionUrl(lastBatchSize);
    lock.lock();
    try {
      int batch;
      for (batch = 0; batch < batches - 1; batch++) {
        downloadBatch(seed, batch * batchSize, batchSize, batchUrl);
      }
      downloadBatch(seed, batch * batchSize, lastBatchSize, lastBatchUrl);
    } catch (final IOException ex) {
      if (getRetryDelayMs() > 0) {
        earliestNextAttempt = CLOCK.instant().plusMillis(getRetryDelayMs());
      }
      throw new SeedException("Failed downloading bytes", ex);
    } catch (final SecurityException ex) {
      // Might be thrown if resource access is restricted (such as in an applet sandbox).
      throw new SeedException("SecurityManager prevented access to a remote seed source", ex);
    } finally {
      lock.unlock();
    }
  }

  protected static int divideRoundingUp(int dividend, int divisor) {
    return (dividend + divisor - 1) / divisor;
  }

  protected static int modRange1ToM(int dividend, int modulus) {
    int result = dividend % modulus;
    if (result == 0) {
      result = modulus;
    }
    return result;
  }

  private void downloadBatch(byte[] seed, int offset, int length, URL batchUrl) throws IOException {
    HttpURLConnection connection = openConnection(batchUrl);
    try {
      downloadBytes(connection, seed, offset, length);
    } finally {
      connection.disconnect();
    }
  }

  @Override public boolean isWorthTrying() {
    return getRetryDelayMs() <= 0 ||
        !earliestNextAttempt.isAfter(RandomDotOrgApi2Client.CLOCK.instant());
  }

  @Override public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WebSeedClient that = (WebSeedClient) o;
    return getRetryDelayMs() == that.getRetryDelayMs() && Objects.equals(getProxy(),
        that.getProxy()) &&
        Objects.equals(getSocketFactory(), that.getSocketFactory()) && userAgent.equals(that.userAgent);
  }

  @Override public int hashCode() {
    return Objects.hash(getProxy(), getSocketFactory(), getRetryDelayMs(), userAgent);
  }

  /**
   * The proxy to use with this server, or null to use the JVM default.
   */
  @Nullable protected Proxy getProxy() {
    return configuration.getProxy();
  }

  /**
   * The SSLSocketFactory to use with this server.
   */
  @Nullable protected SSLSocketFactory getSocketFactory() {
    return configuration.getSocketFactory();
  }

  /**
   * Wait this many milliseconds before trying again after an IOException.
   */
  protected long getRetryDelayMs() {
    return configuration.getRetryDelayMs();
  }
}
