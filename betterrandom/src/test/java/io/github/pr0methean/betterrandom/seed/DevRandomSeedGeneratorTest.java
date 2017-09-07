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
import org.testng.Reporter;
import org.testng.annotations.Test;

/**
 * Unit test for the seed generator that reads data from /dev/random (on platforms that provide
 * it).
 *
 * @author Daniel Dyer
 */
public class DevRandomSeedGeneratorTest {

  @Test(timeOut = 15000)
  public void testGenerator() {
    try {
      final SeedGenerator generator = DevRandomSeedGenerator.DEV_RANDOM_SEED_GENERATOR;
      final byte[] seed = generator.generateSeed(32);
      assert seed.length == 32 : "Failed to generate seed of correct length";
      generator.generateSeed(seed); // Check that other syntax also works
    } catch (final SeedException ex) {
      // This exception is OK, but only if we are running on a platform that
      // does not provide /dev/random.
      assert !new File("/dev/random")
          .exists() : "Seed generator failed even though /dev/random exists.";
      Reporter.log("/dev/random does not exist on this platform.");
    }
  }
}
