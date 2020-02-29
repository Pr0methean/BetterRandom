// ============================================================================
//   Copyright 2006-2012 Daniel W. Dyer
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
// ============================================================================
package io.github.pr0methean.betterrandom.seed;

import com.google.common.primitives.UnsignedBytes;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * <p>Connects to <a href="https://www.random.org/clients/http/" target="_top">random.org's old
 * API</a> (via HTTPS) and downloads a set of random bits to use as seed data.  It is generally
 * better to use the {@link DevRandomSeedGenerator} where possible, as it should be much quicker.
 * This seed generator is most useful on Microsoft Windows without Cygwin, and other platforms that
 * do not provide {@literal /dev/random} (or where that device is very slow).</p>
 * <p>Random.org collects randomness from atmospheric noise using 9 radios, located at undisclosed
 * addresses in Dublin and Copenhagen and tuned to undisclosed AM/FM frequencies. (The secrecy is
 * intended to help prevent tampering with the output using a well-placed radio transmitter, and the
 * use of AM/FM helps ensure that any such tampering would cause illegal interference with
 * broadcasts and quickly attract regulatory attention.)</p>
 * <p>Random.org has two APIs: an <a href="https://www.random.org/clients/http/">old API</a> and a
 * <a href="https://api.random.org/json-rpc/1/">newer JSON-RPC API</a>. Since the new one requires
 * a key obtained from random.org, the old one is used by default. However, if you have a key, you
 * can provide it by calling {@link #setApiKey(UUID)}, and the new API will then be used.</p>
 * <p>Note that when using the old API, random.org limits the supply of free random numbers to any
 * one IP address; if you operate from a fixed address (at least if you use IPv4), you can <a
 * href="https://www.random.org/quota/">check
 * your quota and buy more</a>. On the new API, the quota is per key rather than per IP, and
 * commercial-use pricing follows a <a href="https://api.random.org/pricing">different
 * scheme</a>.</p>
 *
 * @author Daniel Dyer (old API)
 * @author Chris Hennick (new API; refactoring)
 */
public final class RandomDotOrgSeedGenerator extends WebJsonSeedGenerator {
  /**
   * This version of the client may make HTTP requests as fast as your computer is capable of
   * sending them. Since it is inherently spammy, it is recommended only when you know your usage is
   * light and/or no other source of randomness will do.
   */
  public static final RandomDotOrgSeedGenerator RANDOM_DOT_ORG_SEED_GENERATOR
      = new RandomDotOrgSeedGenerator(false);

  /**
   * Upon a failed request, this version of the client waits 10 seconds before trying again. If
   * called again during that waiting period, throws {@link SeedException}. The {@link
   * DefaultSeedGenerator} uses this version.
   */
  public static final RandomDotOrgSeedGenerator DELAYED_RETRY
      = new RandomDotOrgSeedGenerator(true);

  private static final String JSON_REQUEST_FORMAT = "{\"jsonrpc\":\"2.0\"," +
      "\"method\":\"generateBlobs\",\"params\":{\"apiKey\":\"%s\",\"n\":1,\"size\":%d},\"id\":%d}";
  private static final long serialVersionUID = 8901705097958111045L;

  /**
   * The value for the HTTP User-Agent request header for this seed generator's HTTP requests.
   *
   * @return the value for User-Agent
   */
  @Override protected String getUserAgent() {
    return USER_AGENT;
  }

  /**
   * The maximum number of bytes the site will provide in response to one request. Seeds larger than
   * this will be generated using multiple requests.
   *
   * @return the maximum request size in bytes
   */
  @Override protected int getMaxRequestSize() {
    return MAX_REQUEST_SIZE;
  }

  private Object readResolve() {
    return useRetryDelay ? DELAYED_RETRY : RANDOM_DOT_ORG_SEED_GENERATOR;
  }

  private static final AtomicLong REQUEST_ID = new AtomicLong(0);
  private static final AtomicReference<UUID> API_KEY = new AtomicReference<>(null);
  private static final Decoder BASE64 = Base64.getDecoder();
  private static final String BASE_URL = "https://www.random.org";
  /**
   * The URL from which the random bytes are retrieved (old API).
   */
  @SuppressWarnings("HardcodedFileSeparator") private static final String RANDOM_URL =
      BASE_URL + "/integers/?num={0,number,0}&min=0&max=255&col=1&base=16&format=plain&rnd=new";
  private static final String USER_AGENT = RandomDotOrgSeedGenerator.class.getName();
  private static final int MAX_REQUEST_SIZE = 10000;
  private static final URL JSON_REQUEST_URL;

  static {
    try {
      JSON_REQUEST_URL = new URL("https://api.random.org/json-rpc/2/invoke");
    } catch (final MalformedURLException e) {
      // Should never happen.
      throw new InternalError(e);
    }
  }

  RandomDotOrgSeedGenerator(final boolean useRetryDelay) {
    super(useRetryDelay);
  }

  /**
   * Sets the API key. If not null, random.org's JSON API is used. Otherwise, the old API is used.
   *
   * @param apiKey An API key obtained from random.org.
   */
  public static void setApiKey(@Nullable final UUID apiKey) {
    API_KEY.set(apiKey);
  }

  /**
   * Performs a single request for random bytes.
   *
   * @param seed the array to save them to
   * @param offset the first index to save them to in the array
   * @param length the number of bytes to request from random.org
   * @throws IOException If a connection error occurs.
   * @throws SeedException If random.org sends a malformed response body.
   */
  @Override protected void downloadBytes(byte[] seed, int offset, final int length) throws IOException {
    HttpURLConnection connection = null;
    lock.lock();
    try {
      final UUID currentApiKey = API_KEY.get();
      if (currentApiKey == null) {
        // Use old API.
        connection = openConnection(new URL(MessageFormat.format(RANDOM_URL, length)));
        try (final BufferedReader reader = getResponseReader(connection)) {
          for (int index = 0; index < length; index++) {
            final String line = reader.readLine();
            if (line == null) {
              throw new SeedException(String
                  .format("Insufficient data received: expected %d bytes, got %d.", length, index));
            }
            try {
              seed[offset + index] = UnsignedBytes.parseUnsignedByte(line, 16);
            } catch (final NumberFormatException e) {
              throw new SeedException("random.org sent non-numeric data", e);
            }
          }
        }
      } else {
        // Use JSON API.
        connection = openConnection(JSON_REQUEST_URL);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        try (final OutputStream out = connection.getOutputStream()) {
          out.write(String.format(JSON_REQUEST_FORMAT, currentApiKey, length * Byte.SIZE,
              REQUEST_ID.incrementAndGet()).getBytes(StandardCharsets.UTF_8));
        }
        final JSONObject response = parseJsonResponse(connection);
        final Object error = response.get("error");
        if (error != null) {
          throw new SeedException(error.toString());
        }
        final JSONObject result = checkedGetObject(response, "result", JSONObject.class);
        final JSONObject random = checkedGetObject(result, "random", JSONObject.class);
        final Object data = checkedGetObject(random, "data", Object.class);
        final String base64seed =
            ((data instanceof JSONArray) ? ((JSONArray) data).get(0) : data).toString();
        final byte[] decodedSeed;
        try {
          decodedSeed = BASE64.decode(base64seed);
        } catch (IllegalArgumentException e) {
          throw new SeedException(String.format("random.org sent invalid base64 '%s'", base64seed),
              e);
        }
        if (decodedSeed.length < length) {
          throw new SeedException(String
              .format("Too few bytes returned: expected %d bytes, got '%s'", length, base64seed));
        }
        System.arraycopy(decodedSeed, 0, seed, offset, length);
        final Object advisoryDelayMs = result.get("advisoryDelay");
        if (advisoryDelayMs instanceof Number) {
          // Wait RETRY_DELAY or the advisory delay, whichever is shorter
          final int delayMs = Math.min(getRetryDelayMs(), ((Number) advisoryDelayMs).intValue());
          earliestNextAttempt = CLOCK.instant().plusMillis(delayMs);
        }
      }
    } finally {
      lock.unlock();
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  /**
   * Returns "https://www.random.org (with retry delay)" or "https://www.random.org (without retry
   * delay)".
   */
  @Override public String toString() {
    return BASE_URL + (useRetryDelay ? " (with retry delay)" : " (without retry delay)");
  }
}
