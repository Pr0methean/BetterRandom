package io.github.pr0methean.betterrandom.prng;

import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;

public class AesCounterRandom384Test extends AesCounterRandomTest {

  public AesCounterRandom384Test() {
    super(48);
  }

  @BeforeClass
  public void checkRunnable() throws NoSuchAlgorithmException {
    if (Cipher.getMaxAllowedKeyLength("AES") < 256) {
      throw new SkipException(
          "Test can't run without jurisdiction policy files that allow AES-256");
    }
  }
}
