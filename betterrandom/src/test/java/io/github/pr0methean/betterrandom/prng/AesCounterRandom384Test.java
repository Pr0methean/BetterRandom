package io.github.pr0methean.betterrandom.prng;

import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;

public class AesCounterRandom384Test extends AesCounterRandom256Test {

  @BeforeMethod
  public void checkRunnable() throws NoSuchAlgorithmException {
    if (Cipher.getMaxAllowedKeyLength("AES") < 256) {
      throw new SkipException(
          "Test can't run without jurisdiction policy files that allow AES-256");
    }
  }

  @Override protected int getNewSeedLength(final BaseRandom basePrng) {
    return 48;
  }

  @Override public BaseRandom createRng() {
    return new AesCounterRandom(48);
  }
}
