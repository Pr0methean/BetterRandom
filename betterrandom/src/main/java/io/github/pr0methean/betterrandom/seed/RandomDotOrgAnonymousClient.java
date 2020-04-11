package io.github.pr0methean.betterrandom.seed;

import com.google.common.primitives.UnsignedBytes;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

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
 * a key provided to each user by random.org, a client for the old one is still useful for
 * anonymous access. However, if you have a key, you can instead use {@link RandomDotOrgApi2Client}.
 * </p>
 * <p>Note that when using the old API, random.org limits the supply of free random numbers to any
 * one IP address; if you operate from a fixed address (at least if you use IPv4), you can <a
 * href="https://www.random.org/quota/">check
 * your quota and buy more</a>. On the new API, the quota is per key rather than per IP, and
 * commercial-use pricing follows a <a href="https://api.random.org/pricing">different
 * scheme</a>.</p>
 *
 * @author Daniel Dyer (original version)
 * @author Chris Hennick (refactoring)
 */
public class RandomDotOrgAnonymousClient extends WebSeedClient {

  public static final RandomDotOrgAnonymousClient WITH_DELAYED_RETRY = new RandomDotOrgAnonymousClient(true);
  public static final RandomDotOrgAnonymousClient WITHOUT_DELAYED_RETRY = new RandomDotOrgAnonymousClient(false);
  private static final int MAX_REQUEST_SIZE = 10000;

  private Object readResolve() {
    return useRetryDelay ? WITH_DELAYED_RETRY : WITHOUT_DELAYED_RETRY;
  }

  /**
   * The URL from which the random bytes are retrieved (old API).
   */
  @SuppressWarnings("HardcodedFileSeparator") private static final String RANDOM_URL =
      "https://www.random.org/integers/?num={0,number," +
          "0}&min=0&max=255&col=1&base=16&format=plain&rnd=new";

  /**
   * @param useRetryDelay whether to wait 10 seconds before trying again after an IOException
   *     (attempting to use it again before then will automatically fail)
   */
  private RandomDotOrgAnonymousClient(boolean useRetryDelay) {
    super(useRetryDelay);
  }

  @Override protected int getMaxRequestSize() {
    return MAX_REQUEST_SIZE;
  }

  @Override protected URL getConnectionUrl(int numBytes) {
    try {
      return new URL(MessageFormat.format(RANDOM_URL, numBytes));
    } catch (MalformedURLException e) {
      throw new SeedException("Error creating URL", e);
    }
  }

  @Override protected void downloadBytes(HttpURLConnection connection, byte[] seed, int offset,
      int length) throws IOException {
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
  }
}
