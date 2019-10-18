package io.github.pr0methean.betterrandom.prng;

import com.google.common.base.MoreObjects;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import java.security.MessageDigest;
import org.bouncycastle.crypto.engines.TwofishEngine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jcajce.provider.digest.SHA3;

/**
 * A second subclass of {@link CipherCounterRandom}, used to test that abstract class for
 * AES-specific behavior (since it was split off as a parent of {@link AesCounterRandom}).
 */
public class TwoFishCounterRandom extends CipherCounterRandom {
  private static final int LARGE_KEY_LENGTH = 32;
  private static final int SMALL_KEY_LENGTH = 16;
  private static final long serialVersionUID = 919295559867023505L;
  @SuppressWarnings("CanBeFinal") private static int MAX_KEY_LENGTH_BYTES = LARGE_KEY_LENGTH;

  // WARNING: Don't initialize any instance fields at declaration; they may be initialized too late!
  private transient TwofishEngine cipher;

  public TwoFishCounterRandom(byte[] seed) {
    super(seed);
  }

  @Override public int getMaxKeyLengthBytes() {
    return MAX_KEY_LENGTH_BYTES;
  }

  @Override protected int getKeyLength(int inputLength) {
    return (inputLength > MAX_KEY_LENGTH_BYTES) ? MAX_KEY_LENGTH_BYTES :
        ((inputLength >= 24) ? 24 : 16);
  }

  @Override protected int getMinSeedLength() {
    return SMALL_KEY_LENGTH;
  }

  @Override public int getBlocksAtOnce() {
    // FIXME: Some tests fail when this is changed.
    return 1;
  }

  @Override protected MessageDigest createHash() {
    return new SHA3.Digest384();
  }

  @Override protected void createCipher() {
    lock.lock();
    try {
      cipher = new TwofishEngine();
    } finally {
      lock.unlock();
    }
  }

  @Override protected void setKey(byte[] key) {
    cipher.init(true, new KeyParameter(key));
  }

  @Override
  public MoreObjects.ToStringHelper addSubclassFields(final MoreObjects.ToStringHelper original) {
    return original.add("counter", BinaryUtils.convertBytesToHexString(counter))
        .add("cipher", cipher).add("index", index);
  }

  @Override protected void doCipher(byte[] input, byte[] output) {
    cipher.reset();
    cipher.processBlock(input, 0, output, 0);
  }
}
