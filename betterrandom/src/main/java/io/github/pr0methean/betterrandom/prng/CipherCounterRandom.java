package io.github.pr0methean.betterrandom.prng;

import io.github.pr0methean.betterrandom.SeekableRandom;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.Byte16ArrayArithmetic;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

/**
 * <p>Non-linear random number generator based on a cipher that encrypts an incrementing counter.
 * fed. Subclasses must specify the key length for a given total seed length;  When reseeded with a
 * seed of less than the maximum key length, the new seed is combined with the existing key using a
 * hash algorithm specified by the subclass.</p>
 *
 * <p>The counter length is fixed at 16 bytes to make use of {@link Byte16ArrayArithmetic}.</p>
 *
 * <p>All interaction with the cipher is through abstract methods, so that both JCE and other cipher
 * APIs such as Bouncy Castle can be used. If using a JCE cipher, extending {@link AesCounterRandom}
 * may be simpler than extending this class directly.</p>
 *
 * <p>When used with a fixed seed, the maintainer believes this implementation conforms to NIST SP
 * 800-90A Rev. 1 section 10.2.1. However, the reseeding process differs from section 10.2.1.4.</p>
 *
 * @author Daniel Dyer
 * @author Chris Hennick
 */
public abstract class CipherCounterRandom extends BaseRandom implements SeekableRandom {
  /**
   * 128-bit counter. Package-visible for testing. Note to forkers: when running a cipher in ECB
   * mode, this counter's length should equal the cipher's block size.
   */
  static final int COUNTER_SIZE_BYTES = 16;
  private static final long serialVersionUID = -7872636191973295031L;
  protected final byte[] currentBlock;
  protected volatile byte[] counter;
  protected volatile int index;
  protected transient byte[] addendDigits;
  private volatile byte[] counterInput;
  private volatile boolean seeded;
  private transient MessageDigest hash;

  public CipherCounterRandom(byte[] seed) {
    super(seed);
    currentBlock = new byte[getBytesAtOnce()];
  }

  @Override
  public int getNewSeedLength() {
    return getMaxKeyLengthBytes();
  }

  /**
   * Returns the maximum length in bytes of a key for this PRNG's cipher. If the seed is longer than
   * this, part of it becomes the counter's initial value. Otherwise, the full seed becomes the key
   * and the counter is initially zero.
   * @return the maximum length in bytes of a key.
   */
  public abstract int getMaxKeyLengthBytes();

  @Override public void advance(final long delta) {
    if (delta == 0) {
      return;
    }
    final long intsPerBlock = COUNTER_SIZE_BYTES / Integer.BYTES;
    long blocksDelta = delta / intsPerBlock;
    final int deltaWithinBlock = (int) (delta % intsPerBlock) * Integer.BYTES;
    lock.lock();
    try {
      int newIndex = index + deltaWithinBlock;
      if (newIndex >= COUNTER_SIZE_BYTES) {
        newIndex -= COUNTER_SIZE_BYTES;
        blocksDelta++;
      }
      if (newIndex < 0) {
        newIndex += COUNTER_SIZE_BYTES;
        blocksDelta--;
      }
      blocksDelta -= getBlocksAtOnce(); // Compensate for the increment during nextBlock() below
      Byte16ArrayArithmetic.addInto(counter, blocksDelta, addendDigits);
      nextBlock();
      index = newIndex;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Returns the length of the key that should be extracted from a seed of a given length. During
   * the initial seeding, whatever part of the seed does not become the key, becomes the counter's
   * initial value.
   *
   * @param inputLength the length of the whole seed
   * @return the length of the key
   */
  protected abstract int getKeyLength(int inputLength);

  /**
   * Returns how many consecutive values of the counter are encrypted at once, in order to reduce
   * the number of calls to Cipher methods. Each counter value encrypts to yield a "block" of
   * pseudorandom data. Changing this value won't change the output if the cipher is running in
   * block mode, but it may impact performance.
   *
   * @return the number of blocks (counter values) to encrypt at once
   */
  public abstract int getBlocksAtOnce();

  /**
   * Returns the number of random bytes that can be precalculated at once, which is normally
   * {@code getCounterSizeBytes() * getBlocksAtOnce()}.
   * @return the number of random bytes that can be precalculated at once
   */
  protected int getBytesAtOnce() {
    return COUNTER_SIZE_BYTES * getBlocksAtOnce();
  }

  public int getMaxTotalSeedLengthBytes() {
    return getMaxKeyLengthBytes() + COUNTER_SIZE_BYTES;
  }

  @Override protected void initTransientFields() {
    super.initTransientFields();
    addendDigits = new byte[COUNTER_SIZE_BYTES];
    if (counter == null) {
      counter = new byte[COUNTER_SIZE_BYTES];
    }
    if (counterInput == null) {
      counterInput = new byte[getBytesAtOnce()];
    }
    createCipher();
    hash = createHash();
  }

  /**
   * Returns true, because the seed can either be a counter IV plus a key, or just a key.
   * @return true
   */
  @Override
  protected boolean supportsMultipleSeedLengths() {
    return true;
  }

  protected abstract MessageDigest createHash();

  /**
   * Creates the cipher that {@link #doCipher(byte[], byte[])} will invoke. {@link #setKey(byte[])}
   * will be called before the cipher is used.
   */
  protected abstract void createCipher();

  /**
   * Generates BLOCKS_AT_ONCE 128-bit (16-byte) blocks. Copies them to currentBlock.
   * @throws IllegalStateException If there is a problem with the cipher that generates the
   *     random data.
   */
  protected void nextBlock() {
    int blocks = getBlocksAtOnce();
    for (int i = 0; i < blocks; i++) {
      Byte16ArrayArithmetic.addInto(counter, Byte16ArrayArithmetic.ONE);
      System.arraycopy(counter, 0, counterInput, i * COUNTER_SIZE_BYTES, COUNTER_SIZE_BYTES);
    }
    try {
      doCipher(counterInput, currentBlock);
    } catch (final GeneralSecurityException ex) {
      // Should never happen.  If initialisation succeeds without exceptions
      // we should be able to proceed indefinitely without exceptions.
      throw new IllegalStateException("Failed creating next random block.", ex);
    }
  }

  /**
   * Executes the cipher.
   *
   * @param input an array of input whose length is equal to {@link #getBytesAtOnce()}
   * @param output an array of output whose length is equal to {@link #getBytesAtOnce()}
   * @throws ShortBufferException
   * @throws IllegalBlockSizeException
   * @throws BadPaddingException
   */
  protected abstract void doCipher(byte[] input, byte[] output) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException;

  @Override protected final int next(final int bits) {
    lock.lock();
    int result;
    try {
      if ((getBytesAtOnce() - index) < 4) {
        nextBlock();
        index = 0;
      }
      result = BinaryUtils.convertBytesToInt(currentBlock, index);
      index += 4;
    } finally {
      lock.unlock();
    }
    return result >>> (32 - bits);
  }

  /**
   * {@inheritDoc} If the seed is not of the maximum length, it is combined with the existing seed
   * using SHA-256.
   */
  @Override public void setSeed(final byte[] seed) {
    checkNotTooLong(seed);
    final byte[] key;
    if (seed.length == getMaxKeyLengthBytes()) {
      key = seed.clone();
    } else {
      lock.lock();
      boolean weAreSeeded;
      try {
        weAreSeeded = seeded;
      } finally {
        lock.unlock();
      }
      if (weAreSeeded) {
        // Extend the key
        final byte[] newSeed = new byte[this.seed.length + seed.length];
        System.arraycopy(this.seed, 0, newSeed, 0, this.seed.length);
        System.arraycopy(seed, 0, newSeed, this.seed.length, seed.length);
        final int keyLength = getKeyLength(newSeed.length);
        if (newSeed.length > keyLength) {
          final byte[] digest = hash.digest(newSeed);
          key = digest.length > keyLength ? Arrays.copyOf(digest, keyLength) : digest;
        } else {
          key = newSeed;
        }
      } else {
        key = seed.clone();
      }
    }
    lock.lock();
    try {
      setSeedInternal(key);
      entropyBits.addAndGet(8L * (seed.length - key.length));
    } finally {
      lock.unlock();
    }
  }

  private void checkNotTooLong(final byte[] seed) {
    int maxLength = getMaxTotalSeedLengthBytes();
    if (seed.length > maxLength) {
      throw new IllegalArgumentException(String.format(
          "Seed length is %d bytes; maximum is %d bytes", seed.length, maxLength));
    }
  }

  /**
   * Combines the given seed with the existing seed using SHA-256.
   */
  public void setSeed(final long seed) {
    if (superConstructorFinished) {
      setSeed(BinaryUtils.convertLongToBytes(seed));
    }
  }

  @Override protected void setSeedInternal(final byte[] seed) {
    checkNotTooLong(seed);
    if (seed.length < getMinSeedLength()) {
      throw new IllegalArgumentException(String.format(
          "Seed length is %d bytes; need at least 16 bytes", seed.length));
    }
    super.setSeedInternal(seed);
    // determine how much of seed can go to key
    final int keyLength = getKeyLength(seed.length);
    final byte[] key = (seed.length == keyLength) ? seed : Arrays.copyOfRange(seed, 0, keyLength);
    // rest goes to counter
    final int bytesToCopyToCounter = seed.length - keyLength;
    if (bytesToCopyToCounter > 0) {
      System.arraycopy(seed, keyLength, counter, 0, bytesToCopyToCounter);
    }
    Arrays.fill(counter, bytesToCopyToCounter, COUNTER_SIZE_BYTES, (byte) 0);
    try {
      setKey(key);
    } catch (final InvalidKeyException e) {
      throw new InternalError("Invalid key: " + Arrays.toString(key), e);
    }
    index = getBytesAtOnce();
    seeded = true;
  }

  /**
   * Returns the minimum seed length.
   * @return the minimum seed length
   */
  protected abstract int getMinSeedLength();

  /**
   * Sets the key on the cipher. Always called with {@code lock} held.
   *
   * @param key the new key
   * @throws InvalidKeyException if the cipher rejects the key
   */
  protected abstract void setKey(byte[] key) throws InvalidKeyException;
}
