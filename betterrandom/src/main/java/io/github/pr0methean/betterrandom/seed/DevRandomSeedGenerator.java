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

import io.github.pr0methean.betterrandom.util.LogPreFormatter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RNG seed strategy that gets data from {@literal /dev/random} on systems that provide it (e.g.
 * Solaris/Linux).  If {@literal /dev/random} does not exist or is not accessible, a {@link
 * SeedException} is thrown. If {@literal /dev/random} did not exist during a previous call to this
 * method or to {@link #generateSeed(int)}, then for performance reasons, we assume for the rest of
 * the JVM's lifespan that it still doesn't exist.
 *
 * @author Daniel Dyer
 */
public enum DevRandomSeedGenerator implements SeedGenerator {

  /** Singleton instance. */
  DEV_RANDOM_SEED_GENERATOR;

  private static final LogPreFormatter LOG = new LogPreFormatter(DevRandomSeedGenerator.class);
  @SuppressWarnings("HardcodedFileSeparator")
  private static final String DEV_RANDOM_STRING = "/dev/random";
  private static final File DEV_RANDOM = new File(DEV_RANDOM_STRING);
  private static final AtomicBoolean DEV_RANDOM_DOES_NOT_EXIST = new AtomicBoolean(false);

  /**
   * @throws SeedException if {@literal /dev/random} does not exist or is not accessible.
   */
  @Override
  public void generateSeed(final byte[] randomSeed) throws SeedException {
    if (!isWorthTrying()) {
      throw new SeedException(DEV_RANDOM_STRING + " did not exist when previously checked for");
    }

    try (FileInputStream file = new FileInputStream(DEV_RANDOM)) {
      final int length = randomSeed.length;
      int count = 0;
      while (count < length) {
        final int bytesRead = file.read(randomSeed, count, length - count);
        if (bytesRead == -1) {
          throw new SeedException("EOF encountered reading random data.");
        }
        count += bytesRead;
      }
    } catch (final IOException ex) {
      if (!DEV_RANDOM.exists()) {
        LOG.error(DEV_RANDOM_STRING + " does not exist");
        DEV_RANDOM_DOES_NOT_EXIST.lazySet(true);
      }
      throw new SeedException("Failed reading from " + DEV_RANDOM_STRING, ex);
    } catch (final SecurityException ex) {
      // Might be thrown if resource access is restricted (such as in
      // an applet sandbox).
      throw new SeedException("SecurityManager prevented access to " + DEV_RANDOM_STRING, ex);
    }
  }

  @Override
  public boolean isWorthTrying() {
    return !(DEV_RANDOM_DOES_NOT_EXIST.get());
  }

  /** Returns "/dev/random". */
  @Override
  public String toString() {
    return DEV_RANDOM_STRING;
  }
}
