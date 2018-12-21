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

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.Byte16ArrayArithmetic;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.LoggerFactory;

import static io.github.pr0methean.betterrandom.util.Java8Constants.INT_BYTES;

/**
 * <p>CipherCounterRandom using AES (Rijndael).</p> <p>Keys larger than 128 bits, and thus seeds
 * larger than 256 bits, require unlimited strength cryptography policy files on closed-source
 * JDKs.</p> <p><em>NOTE: Because instances of this class
 * require 128-bit seeds, it is not possible to seed this RNG using the {@link #setSeed(long)}
 * method inherited from {@link Random} until the seed array has been set.</em></p>
 * @author Daniel Dyer
 * @author Chris Hennick
 */
public class AesCounterRandom extends CipherCounterRandom {

  private static final long serialVersionUID = 4808258824475143174L;
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

  @Override
  public int getCounterSizeBytes() {
    return COUNTER_SIZE_BYTES;
  }

  @Override
  public int getBlocksAtOnce() {
    return BLOCKS_AT_ONCE;
  }

  @Override
  public int getBytesAtOnce() {
    return BYTES_AT_ONCE;
  }

  @Override
  public int getMaxTotalSeedLengthBytes() {
    return MAX_TOTAL_SEED_LENGTH_BYTES;
  }

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
  @SuppressWarnings("CanBeFinal") private static int MAX_KEY_LENGTH_BYTES = 0;

  static {
    try {
      MAX_KEY_LENGTH_BYTES = Cipher.getMaxAllowedKeyLength(ALGORITHM_MODE) / 8;
    } catch (final GeneralSecurityException e) {
      throw new InternalError(e.getMessage());
    }
    LoggerFactory.getLogger(AesCounterRandom.class)
        .info("Maximum allowed key length for AES is {} bytes", MAX_KEY_LENGTH_BYTES);
    MAX_KEY_LENGTH_BYTES = Math.min(MAX_KEY_LENGTH_BYTES, 32);
    MAX_TOTAL_SEED_LENGTH_BYTES = MAX_KEY_LENGTH_BYTES + COUNTER_SIZE_BYTES;
  }

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
    index = BYTES_AT_ONCE; // force generation of first block on demand
  }

  /**
   * Returns the maximum length in bytes of an AES key, which is {@code
   * Math.min(Cipher.getMaxAllowedKeyLength("AES/ECB/NoPadding") / 8, 32)}. If the seed is longer
   * than this, part of it becomes the counter's initial value. Otherwise, the full seed becomes the
   * AES key and the counter is initially zero.
   * @return the maximum length in bytes of an AES key.
   */
  @Override
  public int getMaxKeyLengthBytes() {
    return MAX_KEY_LENGTH_BYTES;
  }

  @Override protected int getKeyLength(final int inputLength) {
    return (inputLength > MAX_KEY_LENGTH_BYTES) ? MAX_KEY_LENGTH_BYTES
        : ((inputLength >= 24) ? 24 : 16);
  }

  @Override public ToStringHelper addSubclassFields(final ToStringHelper original) {
    return original.add("counter", BinaryUtils.convertBytesToHexString(counter))
        .add("cipher", cipher)
        .add("index", index);
  }

  @Override
  protected MessageDigest createHash() {
    try {
      return MessageDigest.getInstance(HASH_ALGORITHM);
    } catch (NoSuchAlgorithmException e) {
      throw new InternalError("Required hash algorithm missing");
    }
  }

  @Override
  protected Cipher createCipher() {
    try {
      return Cipher.getInstance(ALGORITHM_MODE);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      throw new InternalError("Required cipher missing");
    }
  }

  @Override protected void setSeedInternal(final byte[] seed) {
    if (seed.length < 16) {
      throw new IllegalArgumentException(String.format(
          "Seed length is %d bytes; need at least 16 bytes", seed.length));
    }
    super.setSeedInternal(seed);
  }

  @Override
  protected void setKey(byte[] key) throws InvalidKeyException {
    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, ALGORITHM));
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
    final int deltaWithinBlock = (int) (delta % INTS_PER_BLOCK) * INT_BYTES;
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
      blocksDelta -= BLOCKS_AT_ONCE; // Compensate for the increment during nextBlock() below
      Byte16ArrayArithmetic.addInto(counter, blocksDelta, addendDigits);
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
