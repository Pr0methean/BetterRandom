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

import static io.github.pr0methean.betterrandom.util.Java8Constants.INT_BYTES;
import static io.github.pr0methean.betterrandom.util.Java8Constants.LONG_BYTES;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.SeekableRandom;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.LoggerFactory;

/**
 * <p>Non-linear random number generator based on the AES block cipher in counter mode. Uses the
 * seed as a key to encrypt a 128-bit counter using AES(Rijndael).</p> <p>By default, we only use a
 * 128-bit key for the cipher because any larger key requires the inconvenience of installing the
 * unlimited strength cryptography policy files for the Java platform.  Larger keys may be used (192
 * or 256 bits) but if the cryptography policy files are not installed, a {@link
 * GeneralSecurityException} will be thrown.</p> <p><em>NOTE: Because instances of this class
 * require 128-bit seeds, it is not possible to seed this RNG using the {@link #setSeed(long)}
 * method inherited from {@link Random} until the seed array has been set.</em></p>
 * @author Daniel Dyer
 * @author Chris Hennick
 */
public class AesCounterRandom extends BaseRandom implements SeekableRandom {

  private static final long serialVersionUID = 5949778642428995210L;
  private static final int DEFAULT_SEED_SIZE_BYTES = 32;
  /**
   * Theoretically, the Rijndael algorithm supports key sizes and block sizes of 16, 20, 24, 28 & 32
   * bytes. Thus, if Java contained a full implementation of Rijndael, specifying it would let us
   * support seeds of 16 to 32 and 36, 40, 44, 48, 52, 56, 60 & 64 bytes. However, neither Oracle
   * Java nor OpenJDK provides any implementation of the part of Rijndael that isn't AES.
   */
  private static final String ALGORITHM = "AES";
  @SuppressWarnings("HardcodedFileSeparator") private static final String ALGORITHM_MODE =
      ALGORITHM + "/ECB/NoPadding";
  /**
   * 128-bit counter. Package-visible for testing. Note to forkers: when running a cipher in ECB
   * mode, this counter's length should equal the cipher's block size.
   */
  static final int COUNTER_SIZE_BYTES = 16;
  private static final int INTS_PER_BLOCK = COUNTER_SIZE_BYTES / INT_BYTES;
  /**
   * Number of blocks to encrypt at once, to construct/GC fewer arrays. This takes advantage of the
   * fact that in ECB mode, concatenating and then encrypting gives the same output as encrypting
   * and then concatenating, as long as both plaintexts are a whole number of blocks. (The AES block
   * size is 128 bits at all key lengths.)
   */
  private static final int BLOCKS_AT_ONCE = 16;
  private static final int BYTES_AT_ONCE = COUNTER_SIZE_BYTES * BLOCKS_AT_ONCE;
  private static final String HASH_ALGORITHM = "SHA-256";
  private static final int MAX_TOTAL_SEED_LENGTH_BYTES;
  private static final byte[] ZEROES = new byte[COUNTER_SIZE_BYTES];
  @SuppressWarnings("CanBeFinal") private static int MAX_KEY_LENGTH_BYTES = 0;

  static {
    try {
      MAX_KEY_LENGTH_BYTES = Cipher.getMaxAllowedKeyLength(ALGORITHM_MODE) / 8;
    } catch (final GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
    LoggerFactory.getLogger(AesCounterRandom.class)
        .info("Maximum allowed key length for AES is {} bytes", MAX_KEY_LENGTH_BYTES);
    MAX_KEY_LENGTH_BYTES = Math.min(MAX_KEY_LENGTH_BYTES, 32);
    MAX_TOTAL_SEED_LENGTH_BYTES = MAX_KEY_LENGTH_BYTES + COUNTER_SIZE_BYTES;
  }

  private final byte[] currentBlock;
  // WARNING: Don't initialize any instance fields at declaration; they may be initialized too late!
  @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject") private transient Cipher
      cipher;
  private volatile byte[] counter;
  private volatile byte[] counterInput;
  private volatile boolean seeded;
  private volatile int index;
  private transient byte[] addendDigits;

  /**
   * Creates a new RNG and seeds it using 256 bits from the {@link DefaultSeedGenerator}.
   * @throws SeedException if the {@link DefaultSeedGenerator} fails to generate a seed.
   */
  public AesCounterRandom() throws SeedException {
    this(DEFAULT_SEED_SIZE_BYTES);
  }

  /**
   * Seed the RNG using the provided seed generation strategy to create a 256-bit seed.
   * @param seedGenerator The seed generation strategy that will provide the seed value for this
   *     RNG.
   * @throws SeedException if there is a problem generating a seed.
   */
  public AesCounterRandom(final SeedGenerator seedGenerator) throws SeedException {
    this(seedGenerator.generateSeed(DEFAULT_SEED_SIZE_BYTES));
  }

  /**
   * Seed the RNG using the {@link DefaultSeedGenerator} to create a seed of the specified size.
   * @param seedSizeBytes The number of bytes to use for seed data. Valid values range from 16
   *     to {@link #getMaxKeyLengthBytes()} + 16.
   * @throws SeedException if the {@link DefaultSeedGenerator} fails to generate a seed.
   */
  public AesCounterRandom(final int seedSizeBytes) throws SeedException {
    this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(seedSizeBytes));
  }

  /**
   * Creates an RNG and seeds it with the specified seed data.
   * @param seed The seed data used to initialise the RNG. Length must be at least 16 and no
   *     more than {@link #getMaxKeyLengthBytes()} + 16.
   */
  public AesCounterRandom(final byte[] seed) {
    super(seed);
    currentBlock = new byte[BYTES_AT_ONCE];
    index = BYTES_AT_ONCE; // force generation of first block on demand
  }

  /**
   * Returns the maximum length in bytes of an AES key, which is {@code
   * Math.min(Cipher.getMaxAllowedKeyLength("AES/ECB/NoPadding") / 8, 32)}. If the seed is longer
   * than this, part of it becomes the counter's initial value. Otherwise, the full seed becomes the
   * AES key and the counter is initially zero.
   * @return the maximum length in bytes of an AES key.
   */
  public static int getMaxKeyLengthBytes() {
    return MAX_KEY_LENGTH_BYTES;
  }

  private static int getKeyLength(final byte[] input) {
    return (input.length > MAX_KEY_LENGTH_BYTES) ? MAX_KEY_LENGTH_BYTES
        : ((input.length >= 24) ? 24 : 16);
  }

  @Override public ToStringHelper addSubclassFields(final ToStringHelper original) {
    return original.add("counter", BinaryUtils.convertBytesToHexString(counter))
        .add("cipher", cipher)
        .add("index", index);
  }

  @Override protected void initTransientFields() {
    super.initTransientFields();
    addendDigits = new byte[COUNTER_SIZE_BYTES];
    if (counter == null) {
      counter = new byte[COUNTER_SIZE_BYTES];
    }
    if (counterInput == null) {
      counterInput = new byte[BYTES_AT_ONCE];
    }
    try {
      cipher = Cipher.getInstance(ALGORITHM_MODE);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      throw new RuntimeException("JVM doesn't provide " + ALGORITHM_MODE, e);
    }
  }

  private void incrementCounter() {
    for (int i = 0; i < COUNTER_SIZE_BYTES; i++) {
      ++counter[i];
      if (counter[i] != 0) // Check whether we need to loop again to carry the one.
      {
        break;
      }
    }
  }

  /**
   * Generates BLOCKS_AT_ONCE 128-bit (16-byte) blocks. Copies them to currentBlock.
   * @throws IllegalStateException If there is a problem with the cipher that generates the
   *     random data.
   */
  private void nextBlock() {
    for (int i = 0; i < BLOCKS_AT_ONCE; i++) {
      incrementCounter();
      System.arraycopy(counter, 0, counterInput, i * COUNTER_SIZE_BYTES, COUNTER_SIZE_BYTES);
    }
    try {
      cipher.doFinal(counterInput, 0, COUNTER_SIZE_BYTES * BLOCKS_AT_ONCE, currentBlock);
    } catch (final GeneralSecurityException ex) {
      // Should never happen.  If initialisation succeeds without exceptions
      // we should be able to proceed indefinitely without exceptions.
      throw new IllegalStateException("Failed creating next random block.", ex);
    }
  }

  @Override protected final int next(final int bits) {
    lock.lock();
    int result;
    try {
      if ((BYTES_AT_ONCE - index) < 4) {
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
    try {
      final byte[] key;
      if (seed.length == MAX_KEY_LENGTH_BYTES) {
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
          final int keyLength = getKeyLength(newSeed);
          if (newSeed.length > keyLength) {
            final MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(newSeed);
            key = Arrays.copyOf(md.digest(), keyLength);
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
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private void checkNotTooLong(byte[] seed) {
    if (seed.length > MAX_TOTAL_SEED_LENGTH_BYTES) {
      throw new IllegalArgumentException(String.format(
          "Seed length is %d bytes; maximum is %d bytes", seed.length, MAX_TOTAL_SEED_LENGTH_BYTES));
    }
  }

  /**
   * Combines the given seed with the existing seed using SHA-256.
   */
  @Override @SuppressWarnings("contracts.postcondition.not.satisfied")
  public void setSeed(final long seed) {
    if (superConstructorFinished) {
      final byte[] seedBytes = BinaryUtils.convertLongToBytes(seed);
      setSeed(seedBytes);
    }
  }

  @Override protected void setSeedInternal(final byte[] seed) {
    checkNotTooLong(seed);
    if (seed.length < 16) {
      throw new IllegalArgumentException(String.format(
          "Seed length is %d bytes; need at least 16 bytes", seed.length));
    }
    super.setSeedInternal(seed);
    // determine how much of seed can go to key
    final int keyLength = getKeyLength(seed);
    final byte[] key = Arrays.copyOfRange(seed, 0, keyLength);
    // rest goes to counter
    int bytesToCopyToCounter = seed.length - keyLength;
    System.arraycopy(seed, keyLength, counter, 0, bytesToCopyToCounter);
    System.arraycopy(ZEROES, 0, counter, bytesToCopyToCounter,
        COUNTER_SIZE_BYTES - bytesToCopyToCounter);
    try {
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, ALGORITHM));
    } catch (final InvalidKeyException e) {
      throw new RuntimeException("Invalid key: " + Arrays.toString(key), e);
    }
    if (currentBlock != null) {
      index = BYTES_AT_ONCE;
    } // else it'll be initialized in ctor
    seeded = true;
  }

  /** Returns the longest supported seed length. */
  @Override public int getNewSeedLength() {
    return MAX_TOTAL_SEED_LENGTH_BYTES;
  }

  @Override public void advance(final long delta) {
    if (delta == 0) {
      return;
    }
    long blocksDelta = delta / INTS_PER_BLOCK;
    int deltaWithinBlock = (int) (delta % INTS_PER_BLOCK) * INT_BYTES;
    lock.lock();
    try {
      int newIndex = index + deltaWithinBlock;
      while (newIndex >= COUNTER_SIZE_BYTES) {
        newIndex -= COUNTER_SIZE_BYTES;
        blocksDelta++;
      }
      while (newIndex < 0) {
        newIndex += COUNTER_SIZE_BYTES;
        blocksDelta--;
      }
      blocksDelta -= BLOCKS_AT_ONCE; // Compensate for the increment during nextBlock() below
      System.arraycopy(ZEROES, 0, addendDigits, 0, COUNTER_SIZE_BYTES - LONG_BYTES);
      BinaryUtils.convertLongToBytes(blocksDelta, addendDigits, COUNTER_SIZE_BYTES - LONG_BYTES);
      if (blocksDelta < 0) {
        // Sign extend
        for (int i = 0; i < (COUNTER_SIZE_BYTES - LONG_BYTES); i++) {
          addendDigits[i] = -1;
        }
      }
      boolean carry = false;
      for (int i = 0; i < COUNTER_SIZE_BYTES; i++) {
        final int oldCounterUnsigned = counter[i] < 0 ? counter[i] + 256 : counter[i];
        counter[i] += addendDigits[COUNTER_SIZE_BYTES - i - 1] + (carry ? 1 : 0);
        final int newCounterUnsigned = counter[i] < 0 ? counter[i] + 256 : counter[i];
        carry = (oldCounterUnsigned > newCounterUnsigned)
            || (carry && (oldCounterUnsigned == newCounterUnsigned));
      }
      nextBlock();
      index = newIndex;
    } finally {
      lock.unlock();
    }
  }

  @Override protected boolean supportsMultipleSeedLengths() {
    return true;
  }
}
