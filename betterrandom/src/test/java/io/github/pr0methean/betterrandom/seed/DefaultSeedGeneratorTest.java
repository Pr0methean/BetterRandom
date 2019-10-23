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

import static org.testng.Assert.assertTrue;

import java.security.Permission;
import org.testng.annotations.Test;

/**
 * Unit test for {@link DefaultSeedGenerator}.
 *
 * @author Daniel Dyer
 */
public class DefaultSeedGeneratorTest extends AbstractSeedGeneratorTest {

  public DefaultSeedGeneratorTest() {
    super(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  }

  @Test public void testBasicFunction() throws SeedException {
    SeedTestUtils.testGenerator(seedGenerator, true);
  }

  @Test public void testIsWorthTrying() {
    // Should always be true
    assertTrue(seedGenerator.isWorthTrying());
  }

  /**
   * Check that the default seed generator gracefully falls back to an alternative generation
   * strategy when the security manager prevents it from using its first choice.
   */
  @SuppressWarnings("CallToSystemSetSecurityManager") @Test(timeOut = 120000)
  public void testRestrictedEnvironment() throws SeedException {
    final Thread affectedThread = Thread.currentThread();
    final SecurityManager securityManager = System.getSecurityManager();
    try {
      // Don't allow file system or network access.
      System.setSecurityManager(new RestrictedSecurityManager(affectedThread));
      seedGenerator.generateSeed(new byte[4]);
      // Should get to here without exceptions.
    } finally {
      // Restore the original security manager so that we don't
      // interfere with the running of other tests.
      System.setSecurityManager(securityManager);
    }
  }

  /**
   * This security manager allows everything except for some operations that are explicitly blocked.
   * These operations are accessing /dev/random and opening a socket connection.
   */
  @SuppressWarnings({"CustomSecurityManager", "EqualityOperatorComparesObjects"})
  private static final class RestrictedSecurityManager extends SecurityManager {

    private final Thread affectedThread;

    private RestrictedSecurityManager(final Thread affectedThread) {
      this.affectedThread = affectedThread;
    }

    @SuppressWarnings({"HardcodedFileSeparator"}) @Override
    public void checkRead(final String file) {
      if ((Thread.currentThread() == affectedThread) && "/dev/random".equals(file)) {
        throw new SecurityException("Test not permitted to access /dev/random");
      }
    }

    @Override
    public void checkConnect(final String host, final int port) {
      if (Thread.currentThread() == affectedThread) {
        throw new SecurityException("Test not permitted to connect to " + host + ':' + port);
      }
    }

    @Override public void checkPermission(final Permission permission) {
      // Allow everything.
    }
  }
}
