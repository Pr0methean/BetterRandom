package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.seed.SeedException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;

public class AesCounterRandom384Test extends AesCounterRandom256Test {

  private static final boolean UNLIMITED_STRENGTH_CRYPTO;

  static {
    try {
      UNLIMITED_STRENGTH_CRYPTO = Cipher.getMaxAllowedKeyLength("AES") >= 256;
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public BaseEntropyCountingRandom createRng() {
    if (UNLIMITED_STRENGTH_CRYPTO) {
      try {
        return new AesCounterRandom(48);
      } catch (SeedException e) {
        throw new RuntimeException(e);
      }
    } else {
      return super.createRng();
    }
  }
}
