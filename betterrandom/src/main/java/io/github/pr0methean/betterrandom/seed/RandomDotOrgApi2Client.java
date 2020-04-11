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

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Uses the random.org JSON API documented at
 * <a href="https://api.random.org/json-rpc/1/">api.random.org</a>. This allows some customers to
 * get a larger volume of random numbers from random.org than they can with
 * {@link RandomDotOrgAnonymousClient}, especially when there are many other clients using the old
 * API on the same IP address. The source and quality of the random numbers is the same.
 */
public final class RandomDotOrgApi2Client extends WebSeedClient {

  private static final String JSON_REQUEST_FORMAT = "{\"jsonrpc\":\"2.0\"," +
      "\"method\":\"generateBlobs\",\"params\":{\"apiKey\":\"%s\",\"n\":1,\"size\":%d},\"id\":%d}";
  private static final long serialVersionUID = 8901705097958111045L;

  /**
   * The maximum number of bytes the site will provide in response to one request. Seeds larger than
   * this will be generated using multiple requests.
   *
   * @return the maximum request size in bytes
   */
  @Override protected int getMaxRequestSize() {
    return MAX_REQUEST_SIZE;
  }

  @Override protected URL getConnectionUrl(int numBytes) {
    return JSON_REQUEST_URL;
  }

  private static final AtomicLong REQUEST_ID = new AtomicLong(0);
  private static final AtomicReference<UUID> API_KEY = new AtomicReference<>(null);
  private static final Decoder BASE64 = Base64.getDecoder();
  private static final String BASE_URL = "https://www.random.org";
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

  private final UUID apiKey;

  RandomDotOrgApi2Client(final boolean useRetryDelay, UUID apiKey) {
    super(useRetryDelay);
    if (apiKey == null) {
      throw new IllegalArgumentException("apiKey must not be null");
    }
    this.apiKey = apiKey;
  }

  /**
   * Performs a single request for random bytes.
   *
   * @param connection the connection to download from
   * @param seed the array to save them to
   * @param offset the first index to save them to in the array
   * @param length the number of bytes to request from random.org
   * @throws IOException If a connection error occurs.
   * @throws SeedException If random.org sends a malformed response body.
   */
  @Override protected void downloadBytes(HttpURLConnection connection, byte[] seed, int offset,
      final int length) throws IOException {
    connection.setDoOutput(true);
    connection.setRequestMethod("POST");
    try (final OutputStream out = connection.getOutputStream()) {
      out.write(String.format(JSON_REQUEST_FORMAT, apiKey, length * Byte.SIZE,
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
    if (useRetryDelay && advisoryDelayMs instanceof Number) {
      // Wait RETRY_DELAY or the advisory delay, whichever is shorter
      final long delayMs = Math.min(getRetryDelayMs(), ((Number) advisoryDelayMs).longValue());
      earliestNextAttempt = CLOCK.instant().plusMillis(delayMs);
    }
  }

  /**
   * Returns "https://www.random.org (with retry delay)" or "https://www.random.org (without retry
   * delay)".
   */
  @Override public String toString() {
    return BASE_URL + (useRetryDelay ? " (with retry delay)" : " (without retry delay)");
  }

  @Override public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    RandomDotOrgApi2Client that = (RandomDotOrgApi2Client) o;
    return apiKey.equals(that.apiKey);
  }

  @Override public int hashCode() {
    return Objects.hash(super.hashCode(), apiKey);
  }
}
