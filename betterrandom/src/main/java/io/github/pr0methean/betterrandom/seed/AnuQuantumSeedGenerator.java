package io.github.pr0methean.betterrandom.seed;

import io.github.pr0methean.betterrandom.util.BinaryUtils;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * API client for the Australian National University's <a href="https://qrng.anu.edu.au/">quantum
 * RNG</a>, which extracts randomness from quantum-vacuum fluctuations. Unlike random.org, this API
 * has no usage quotas; the generator produces 5.7 Gbps, so the output rate is limited only by
 * network bandwidth.
 */
public class AnuQuantumSeedGenerator extends WebJsonSeedGenerator {

  /** Singleton instance. */
  public static final AnuQuantumSeedGenerator ANU_QUANTUM_SEED_GENERATOR = new AnuQuantumSeedGenerator();

  private static final int MAX_STRINGS_PER_REQUEST = 1024;
  private static final int MAX_BYTES_PER_STRING = 1024;

  private static final String USER_AGENT = AnuQuantumSeedGenerator.class.getName();

  private static final String REQUEST_URL_FORMAT
      = "https://qrng.anu.edu.au/API/jsonI.php?length=%d&type=hex16&size=%d";

  protected Object readResolve() {
    return ANU_QUANTUM_SEED_GENERATOR;
  }

  private AnuQuantumSeedGenerator() {
    super(false);
  }

  @Override protected String getUserAgent() {
    return USER_AGENT;
  }

  @Override protected int getMaxRequestSize() {
    return MAX_STRINGS_PER_REQUEST * MAX_BYTES_PER_STRING;
  }

  @Override protected void downloadBytes(byte[] seed, int offset, int length) throws IOException {
    int stringCount = length / MAX_BYTES_PER_STRING;
    int usedLengthOfLastString = length % MAX_BYTES_PER_STRING;
    if (usedLengthOfLastString == 0) {
      usedLengthOfLastString = MAX_BYTES_PER_STRING;
    } else {
      stringCount++;
    }
    int stringLength = (stringCount > 1) ? MAX_BYTES_PER_STRING : length;
    lock.lock();
    try {
      final HttpURLConnection connection = openConnection(
          new URL(String.format(REQUEST_URL_FORMAT, stringCount, stringLength)));
      final JSONObject response = parseJsonResponse(connection);
      final JSONArray byteStrings = checkedGetObject(response, "data", JSONArray.class);
      if (byteStrings.size() != stringCount) {
        throw new SeedException(String.format(
            "qrng.anu.edu.au returned wrong size array (was %d, expected %d)",
            byteStrings.size(), stringCount));
      }
      try {
        for (int stringIndex = 0; stringIndex < stringCount - 1; stringIndex++) {
          BinaryUtils.convertHexStringToBytes(getStringAndCheckLength(byteStrings, stringIndex, 2 * stringLength), seed,
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
    } finally {
      lock.unlock();
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
