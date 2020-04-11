package io.github.pr0methean.betterrandom.seed;

import io.github.pr0methean.betterrandom.util.BinaryUtils;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import javax.annotation.Nullable;
import javax.net.ssl.SSLSocketFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * API client for the Australian National University's <a href="https://qrng.anu.edu.au/">quantum
 * RNG</a>, which extracts randomness from quantum-vacuum fluctuations. Unlike random.org, this API
 * has no usage quotas; the generator produces 5.7 Gbps, so the output rate is limited only by
 * network bandwidth.
 */
public class AnuQuantumSeedClient extends WebSeedClient {

  private static final int MAX_STRINGS_PER_REQUEST = 1024;
  private static final int MAX_BYTES_PER_STRING = 1024;

  private static final String REQUEST_URL_FORMAT
      = "https://qrng.anu.edu.au/API/jsonI.php?length=%d&type=hex16&size=%d";
  private static final long serialVersionUID = -7067446291370465008L;

  public AnuQuantumSeedClient(final boolean useRetryDelay) {
    this(null, null, useRetryDelay);
  }

  public AnuQuantumSeedClient(@Nullable final Proxy proxy,
      @Nullable final SSLSocketFactory socketFactory, final boolean useRetryDelay) {
    super(proxy, socketFactory, useRetryDelay);
  }

  @Override protected int getMaxRequestSize() {
    return MAX_STRINGS_PER_REQUEST * MAX_BYTES_PER_STRING;
  }

  @Override protected URL getConnectionUrl(int numBytes) {
    int stringCount = divideRoundingUp(numBytes, MAX_BYTES_PER_STRING);
    int stringLength = (stringCount > 1) ? MAX_BYTES_PER_STRING : numBytes;
    try {
      return new URL(String.format(REQUEST_URL_FORMAT, stringCount, stringLength));
    } catch (MalformedURLException e) {
      throw new SeedException("Error creating URL", e);
    }
  }

  @Override protected void downloadBytes(HttpURLConnection connection, byte[] seed, int offset,
      int length) throws IOException {
    final JSONObject response = parseJsonResponse(connection);
    final JSONArray byteStrings = checkedGetObject(response, "data", JSONArray.class);
    final int stringCount, stringLength, usedLengthOfLastString;
    if (length > MAX_BYTES_PER_STRING) {
      stringCount = divideRoundingUp(length, MAX_BYTES_PER_STRING);
      stringLength = MAX_BYTES_PER_STRING;
      usedLengthOfLastString = modRange1ToM(length, stringLength);
    } else {
      stringCount = 1;
      stringLength = length;
      usedLengthOfLastString = length;
    }
    if (stringCount != byteStrings.size()) {
      throw new SeedException(String.format("Wrong size response (expected %d byte arrays, got %d",
          stringCount, byteStrings.size()));
    }
    try {
      for (int stringIndex = 0; stringIndex < stringCount - 1; stringIndex++) {
        BinaryUtils.convertHexStringToBytes(
            getStringAndCheckLength(byteStrings, stringIndex, 2 * stringLength),
            seed,
            offset + stringIndex * stringLength);
      }
      BinaryUtils.convertHexStringToBytes(
          getStringAndCheckLength(byteStrings, stringCount - 1, 2 * stringLength)
              .substring(0, usedLengthOfLastString * 2),
          seed, offset + stringLength * (stringCount - 1));
      connection.disconnect();
    } catch (IllegalArgumentException e) {
      throw new SeedException("qrng.anu.edu.au returned malformed JSON", e);
    }
  }

  private String getStringAndCheckLength(JSONArray array, int index, int expectedLength) {
    String out = array.get(index).toString();
    int actualLength = out.length();
    if (actualLength != expectedLength) {
      throw new SeedException(String.format(
          "qrng.anu.edu.au sent string with wrong length (expected %d, was %d)",
          expectedLength, actualLength));
    }
    return out;
  }
}
