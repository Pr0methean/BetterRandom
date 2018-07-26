package io.github.pr0methean.betterrandom.prng;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Cipher;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.testng.IObjectFactory;
import org.testng.annotations.Factory;
import org.testng.annotations.ObjectFactory;

public class AesCounterRandomTestFactory {

  @ObjectFactory IObjectFactory getObjectFactory() {
    return new PowerMockObjectFactory();
  }

  @Factory public static Object[] getInstances() throws NoSuchAlgorithmException {
    int[] desiredSeedSizes = {16, 17, 32, 33, 48};
    int maxSize = Cipher.getMaxAllowedKeyLength("AES") / 8
        + AesCounterRandom.COUNTER_SIZE_BYTES;
    List<AesCounterRandomTest> instances = new ArrayList<>(5);
    for (int size : desiredSeedSizes) {
      if (size > maxSize) {
        break;
      } else {
        instances.add(new AesCounterRandomTest(size));
      }
    }
    return instances.toArray();
  }
}
