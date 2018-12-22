package io.github.pr0methean.betterrandom.prng;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.LoggerFactory;

/**
 * A second subclass of {@link CipherCounterRandom}, used to test that abstract class for
 * AES-specific behavior (since it was split off as a parent of {@link AesCounterRandom}).
 */
public class ChaCha20CounterRandom extends CipherCounterRandom {
  private static final Provider BOUNCY_CASTLE = new BouncyCastleProvider();
  private static final int LARGE_KEY_LENGTH = 32;
  private static final int SMALL_KEY_LENGTH = 16;
  private static final String ALGORITHM_MODE = "CHACHA/ECB/NoPadding";
  @SuppressWarnings("CanBeFinal") private static int MAX_KEY_LENGTH_BYTES = 0;

  static {
    try {
      MAX_KEY_LENGTH_BYTES = Cipher.getMaxAllowedKeyLength(ALGORITHM_MODE) / 8;
    } catch (final GeneralSecurityException e) {
      throw new InternalError(e);
    }
    LoggerFactory.getLogger(AesCounterRandom.class)
        .info("Maximum allowed key length for ChaCha is {} bytes", MAX_KEY_LENGTH_BYTES);
    MAX_KEY_LENGTH_BYTES = Math.min(MAX_KEY_LENGTH_BYTES, LARGE_KEY_LENGTH);
  }

  public ChaCha20CounterRandom(byte[] seed) {
    super(seed);
  }

  @Override
  public int getMaxKeyLengthBytes() {
    return MAX_KEY_LENGTH_BYTES;
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
      return Cipher.getInstance(ALGORITHM_MODE, BOUNCY_CASTLE);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  protected void setKey(byte[] key) throws InvalidKeyException {
    cipher = createCipher();
    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "CHACHA"));
  }
}
