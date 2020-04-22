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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;

/**
 * RNG seed strategy that gets data from {@code /dev/random} on systems that provide it (e.g.
 * Solaris/Linux).  If {@code /dev/random} does not exist or is not accessible, a {@link
 * SeedException} is thrown. If it didn't exist during a previous call to this method or to {@link
 * #generateSeed(int)}, then for performance reasons, we assume for the rest of the JVM's lifespan
 * that it still doesn't exist.
 *
 * @author Daniel Dyer
 */
public enum DevRandomSeedGenerator implements SeedGenerator {

  /**
   * Singleton instance.
   */
  DEV_RANDOM_SEED_GENERATOR;

  @SuppressWarnings("HardcodedFileSeparator") private static final String DEV_RANDOM_STRING =
      "/dev/random";
  private static final File DEV_RANDOM = new File(DEV_RANDOM_STRING);
  @Nullable private static final InputStream inputStream;
  @Nullable private static final Throwable initException;

  static {
    InputStream maybeStream;
    Throwable maybeInitException;
    try {
      maybeStream = new FileInputStream(DEV_RANDOM);
      maybeInitException = null;
    } catch (Throwable t) {
      maybeStream = null;
      maybeInitException = t;
    }
    inputStream = maybeStream;
    initException = maybeInitException;
  }

  /**
   * @throws SeedException if {@literal /dev/random} does not exist or is not accessible.
   */
  @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod") @Override public void generateSeed(
      final byte[] randomSeed) throws SeedException {
    if (!isWorthTrying()) {
      throw new SeedException(DEV_RANDOM_STRING + " does not exist", initException);
    }
    try {
      final int length = randomSeed.length;
      int count = 0;
      while (count < length) {
        final int bytesRead = inputStream.read(randomSeed, count, length - count);
        if (bytesRead == -1) {
          throw new SeedException("EOF encountered reading random data.");
        }
        count += bytesRead;
      }
    } catch (final IOException ex) {
      throw new SeedException("Failed reading from " + DEV_RANDOM_STRING, ex);
    } catch (final SecurityException ex) {
      // Might be thrown if resource access is restricted (such as in
      // an applet sandbox).
      throw new SeedException("SecurityManager prevented access to " + DEV_RANDOM_STRING, ex);
    }
  }

  @Override public boolean isWorthTrying() {
    return inputStream != null;
  }

  /**
   * Returns "/dev/random".
   */
  @Override public String toString() {
    return DEV_RANDOM_STRING;
  }
}
