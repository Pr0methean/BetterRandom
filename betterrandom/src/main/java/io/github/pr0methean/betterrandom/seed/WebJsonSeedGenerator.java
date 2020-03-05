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
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public abstract class WebJsonSeedGenerator implements SeedGenerator {
  /**
   * Measures the retry delay. A ten-second delay might become either nothing or an hour if we used
   * local time during the start or end of Daylight Saving Time, but it's fine if we occasionally
   * wait 9 or 11 seconds instead of 10 because of a leap-second adjustment. See <a
   * href="https://www.youtube.com/watch?v=-5wpm-gesOY">Tom Scott's video</a> about the various
   * considerations involved in this choice of clock.
   */
  protected static final Clock CLOCK = Clock.systemUTC();
  protected static final JSONParser JSON_PARSER = new JSONParser();
  private static final int RETRY_DELAY_MS = 10000;
  private static final Duration RETRY_DELAY = Duration.ofMillis(RETRY_DELAY_MS);
  protected final Lock lock = new ReentrantLock();
  protected volatile Instant earliestNextAttempt = Instant.MIN;
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

  /**
   * @param useRetryDelay whether to wait 10 seconds before trying again after an IOException
   *     (attempting to use it again before then will automatically fail)
   */
  public WebJsonSeedGenerator(final boolean useRetryDelay) {
    this.useRetryDelay = useRetryDelay;
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
    final JSONObject response;
    try (final BufferedReader reader = getResponseReader(connection)) {
      response = (JSONObject) JSON_PARSER.parse(reader);
    } catch (final ParseException e) {
      throw new SeedException("Unparseable JSON response", e);
    }
    return response;
  }

  /**
   * Only has an effect if {@link #useRetryDelay} is true. The delay after an {@link IOException}
   * during which any further attempt to generate a seed will automatically fail without opening
   * another connection.
   *
   * @return the retry delay
   */
  public Duration getRetryDelay() {
    return RETRY_DELAY;
  }

  /**
   * Same as {@link #getRetryDelay()} but expressed as a number of milliseconds.
   *
   * @return the retry delay in milliseconds
   */
  public int getRetryDelayMs() {
    return RETRY_DELAY_MS;
  }

  /**
   * Returns the value to use for the User-Agent HTTP request header.
   * @return the user agent string
   */
  protected abstract String getUserAgent();

  /**
   * Returns the maximum number of bytes that can be obtained with one request to the service.
   * When a seed larger than this is needed, it is obtained using multiple requests.
   *
   * @return the maximum number of bytes per request
   */
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

  /**
   * Opens an {@link HttpURLConnection} that will make a GET request to the given URL using this
   * seed generator's current {@link Proxy}, {@link SeedGenerator} and User-Agent string, with the
   * header {@code Content-Type: application/json}.
   *
   * @param url the URL to connect to
   * @return a connection to the URL
   * @throws IOException if thrown by {@link URL#openConnection()} or {@link URL#openConnection(Proxy)}
   */
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

  /**
   * Downloads random bytes into the given range of a byte array, using one request.
   *
   * @param seed the array to populate
   * @param offset the first index to populate
   * @param length the number of bytes to download
   * @throws IOException if unable to connect to the Web service
   */
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
