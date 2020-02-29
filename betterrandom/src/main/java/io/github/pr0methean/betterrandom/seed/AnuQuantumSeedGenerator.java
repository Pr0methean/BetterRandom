package io.github.pr0methean.betterrandom.seed;

import io.github.pr0methean.betterrandom.util.BinaryUtils;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class AnuQuantumSeedGenerator extends WebJsonSeedGenerator {

  /** Singleton instance. */
  public static final AnuQuantumSeedGenerator ANU_QUANTUM_SEED_GENERATOR = new AnuQuantumSeedGenerator();

  private static final int MAX_STRINGS_PER_REQUEST = 1024;
  private static final int MAX_BYTES_PER_STRING = 1024;

  private static final String USER_AGENT = AnuQuantumSeedGenerator.class.getName();

  private static final String REQUEST_URL_FORMAT
      = "https://qrng.anu.edu.au/API/jsonI.php?length=%d&type=hex16&size=%d";

  private Object readResolve() {
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
      HttpURLConnection connection = openConnection(
          new URL(String.format(REQUEST_URL_FORMAT, stringCount, stringLength)));
      final JSONObject response = parseJsonResponse(connection);
      final JSONArray byteStrings = checkedGetObject(response, "data", JSONArray.class);
      for (int stringIndex = 0; stringIndex < stringCount - 1; stringIndex++) {
        BinaryUtils.convertHexStringToBytes(byteStrings.get(stringIndex).toString(), seed,
            offset + stringIndex * stringLength);
      }
      BinaryUtils.convertHexStringToBytes(
          byteStrings.get(stringCount - 1).toString().substring(0, usedLengthOfLastString * 2),
          seed,
          offset + stringLength * (stringCount - 1));
    } finally {
      lock.unlock();
    }
  }
}
