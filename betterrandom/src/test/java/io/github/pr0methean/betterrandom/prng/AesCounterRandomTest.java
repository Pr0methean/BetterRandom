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
package io.github.pr0methean.betterrandom.prng;

import java.lang.reflect.Constructor;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Cipher;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

/**
 * Unit test for the AES RNG.
 * @author Daniel Dyer
 * @author Chris Hennick
 */
@Test(testName = "AesCounterRandom")
public class AesCounterRandomTest extends AbstractAesCounterRandomTest {

  public AesCounterRandomTest(int seedSizeBytes) {
    super(seedSizeBytes);
  }

  @Factory public static Object[] getInstances()
      throws NoSuchAlgorithmException, NoSuchMethodException {
    Constructor constructor = AesCounterRandomTest.class.getConstructor(int.class);
    PowerMockObjectFactory factory = new PowerMockObjectFactory();
    int[] desiredSeedSizes = {16, 17, 32, 33, 48};
    int maxSize = Cipher.getMaxAllowedKeyLength("AES") / 8
        + AesCounterRandom.COUNTER_SIZE_BYTES;
    List<Object> instances = new ArrayList<>(5);
    for (int size : desiredSeedSizes) {
      if (size > maxSize) {
        break;
      } else {
        instances.add(factory.newInstance(constructor, size));
      }
    }
    return instances.toArray();
  }
}
