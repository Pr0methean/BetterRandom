package io.github.pr0methean.betterrandom.prng;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * A second subclass of {@link CipherCounterRandom}, used to test that abstract class for
 * AES-specific behavior (since it was split off as a parent of {@link AesCounterRandom}).
 */
public class ChaCha20CounterRandom extends CipherCounterRandom {
  private static final Provider BOUNCY_CASTLE = new BouncyCastleProvider();
  private static final int LARGE_KEY_LENGTH = 32;
  private static final int SMALL_KEY_LENGTH = 16;


  public ChaCha20CounterRandom(byte[] seed) {
    super(seed);
  }

  @Override
  public int getMaxKeyLengthBytes() {
    return LARGE_KEY_LENGTH;
  }

  @Override
  protected int getKeyLength(int inputLength) {
    return inputLength >= LARGE_KEY_LENGTH ? LARGE_KEY_LENGTH : SMALL_KEY_LENGTH;
  }

  @Override
  protected int getMinSeedLength() {
    return 16;
  }

  @Override
  public int getCounterSizeBytes() {
    return 64;
  }

  @Override
  public int getBlocksAtOnce() {
    return 1;
  }

  @Override
  protected MessageDigest createHash() {
    return new SHA3.Digest256();
  }

  @Override
  protected Cipher createCipher() {
    try {
      return Cipher.getInstance("CHACHA/ECB/NoPadding", BOUNCY_CASTLE);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  protected void setKey(byte[] key) throws InvalidKeyException {
    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "CHACHA"));
  }

  @Override
  public int getNewSeedLength() {
    return 0;
  }
}
