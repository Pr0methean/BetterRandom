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

import static org.checkerframework.checker.nullness.NullnessUtil.castNonNull;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.LogPreFormatter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

/**
 * <p>Non-linear random number generator based on the AES block cipher in counter mode. Uses the
 * seed as a key to encrypt a 128-bit counter using AES(Rijndael).</p> <p>By default, we only use a
 * 128-bit key for the cipher because any larger key requires the inconvenience of installing the
 * unlimited strength cryptography policy files for the Java platform.  Larger keys may be used (192
 * or 256 bits) but if the cryptography policy files are not installed, a {@link
 * GeneralSecurityException} will be thrown.</p> <p><em>NOTE: Because instances of this class
 * require 128-bit seeds, it is not possible to seed this RNG using the {@link #setSeed(long)}
 * method inherited from {@link java.util.Random} until the seed array has been set.</em></p>
 *
 * @author Daniel Dyer
 * @version $Id: $Id
 */
public class AesCounterRandom extends BaseEntropyCountingRandom {

  private static final long serialVersionUID = 5949778642428995210L;
  private static final LogPreFormatter LOG = new LogPreFormatter(AesCounterRandom.class);
  private static final int DEFAULT_SEED_SIZE_BYTES = 32;
  /**
   * Theoretically, the Rijndael algorithm supports key sizes and block sizes of 16, 20, 24, 28 & 32
   * bytes. Thus, if Java contained a full implementation of Rijndael, specifying it would let us
   * support seeds of 16 to 32 and 36, 40, 44, 48, 52, 56, 60 & 64 bytes. However, neither Oracle
   * Java nor OpenJDK provides any implementation of the part of Rijndael that isn't AES.
   */
  private static final String ALGORITHM = "AES";
  @SuppressWarnings("HardcodedFileSeparator")
  private static final String ALGORITHM_MODE = ALGORITHM + "/ECB/NoPadding";
  /**
   * 128-bit counter. Note to forkers: when running a cipher in ECB mode, this counter's length
   * should equal the cipher's block size.
   */
  private static final int COUNTER_SIZE_BYTES = 16;
  /**
   * Number of blocks to encrypt at once, to construct/GC fewer arrays. This takes advantage of the
   * fact that in ECB mode, concatenating and then encrypting gives the same output as encrypting
   * and then concatenating, as long as both plaintexts are a whole number of blocks. (The AES block
   * size is 128 bits at all key lengths.)
   */
  private static final int BLOCKS_AT_ONCE = 16;
  private static final String HASH_ALGORITHM = "SHA-256";
  private static final int MAX_TOTAL_SEED_LENGTH_BYTES;
  @SuppressWarnings("CanBeFinal")
  private static int MAX_KEY_LENGTH_BYTES = 0;

  static {
    try {
      MAX_KEY_LENGTH_BYTES = Cipher.getMaxAllowedKeyLength(ALGORITHM_MODE) / 8;
    } catch (final GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
    LOG.info("Maximum allowed key length for AES is %d bytes", MAX_KEY_LENGTH_BYTES);
    MAX_KEY_LENGTH_BYTES = Math.min(MAX_KEY_LENGTH_BYTES, 32);
    MAX_TOTAL_SEED_LENGTH_BYTES = MAX_KEY_LENGTH_BYTES + COUNTER_SIZE_BYTES;
  }

  private final byte[] currentBlock;
  // WARNING: Don't initialize any instance fields at declaration; they may be initialized too late!
  @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
  private transient Cipher cipher;
  private byte[] counter;
  private byte[] counterInput;
  private boolean seeded;
  private int index;

  /**
   * Creates a new RNG and seeds it using 256 bits from the default seeding strategy.
   *
   * @throws io.github.pr0methean.betterrandom.seed.SeedException if any.
   */
  public AesCounterRandom() throws SeedException {
    this(DEFAULT_SEED_SIZE_BYTES);
  }

  /**
   * Seed the RNG using the provided seed generation strategy to create a 128-bit seed.
   *
   * @param seedGenerator The seed generation strategy that will provide the seed value for this
   *     RNG.
   * @throws io.github.pr0methean.betterrandom.seed.SeedException If there is a problem
   *     generating a seed.
   */
  public AesCounterRandom(final SeedGenerator seedGenerator) throws SeedException {
    this(seedGenerator.generateSeed(DEFAULT_SEED_SIZE_BYTES));
  }

  /**
   * Seed the RNG using the default seed generation strategy to create a seed of the specified
   * size.
   *
   * @param seedSizeBytes The number of bytes to use for seed data.  Valid values are 16 (128
   *     bits), 24 (192 bits) and 32 (256 bits).  Any other values will result in an exception from
   *     the AES implementation.
   * @throws io.github.pr0methean.betterrandom.seed.SeedException if any.
   * @since 1.0.2
   */
  public AesCounterRandom(final int seedSizeBytes) throws SeedException {
    this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR.generateSeed(seedSizeBytes));
  }

  /**
   * Creates an RNG and seeds it with the specified seed data.
   *
   * @param seed The seed data used to initialise the RNG.
   */
  public AesCounterRandom(final byte[] seed) {
    super(seed);
    currentBlock = new byte[COUNTER_SIZE_BYTES * BLOCKS_AT_ONCE];
    index = currentBlock.length; // force generation of first block on demand
    initTransientFields();
    setSeedInternal(seed);
  }

  /**
   * <p>getMaxKeyLengthBytes.</p>
   *
   * @return If the seed is longer than this, part of it becomes the counter's initial value.
   *     Otherwise, the full seed becomes the AES key and the counter is initially zero. Public for
   *     testing of its initialization.
   */
  public static int getMaxKeyLengthBytes() {
    return MAX_KEY_LENGTH_BYTES;
  }

  private static int getKeyLength(final byte[] input) {
    return input.length > MAX_KEY_LENGTH_BYTES ? MAX_KEY_LENGTH_BYTES
        : input.length >= 24 ? 24 : 16;
  }

  /** {@inheritDoc} */
  @Override
  public ToStringHelper addSubSubclassFields(final ToStringHelper original) {
    return original
        .add("counter", BinaryUtils.convertBytesToHexString(counter))
        .add("cipher", cipher);
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    initTransientFields();
    setSeedInternal(seed);
  }

  /**
   * Called in constructor and readObject to initialize transient fields.
   */
  @EnsuresNonNull({"counter", "counterInput", "cipher", "lock", "longSeedArray", "longSeedBuffer"})
  protected void initTransientFields(
      @UnknownInitialization AesCounterRandom this) {
    super.initTransientFields();
    if (counter == null) {
      counter = new byte[COUNTER_SIZE_BYTES];
    }
    if (counterInput == null) {
      counterInput = new byte[COUNTER_SIZE_BYTES * BLOCKS_AT_ONCE];
    }
    try {
      cipher = Cipher.getInstance(ALGORITHM_MODE);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      throw new RuntimeException("JVM doesn't provide " + ALGORITHM_MODE, e);
    }
  }

  private void incrementCounter() {
    for (int i = 0; i < counter.length; i++) {
      ++counter[i];
      if (counter[i] != 0) // Check whether we need to loop again to carry the one.
      {
        break;
      }
    }
  }

  /**
   * Generates BLOCKS_AT_ONCE 128-bit (16-byte) blocks. Copies them to currentBlock.
   *
   * @throws GeneralSecurityException If there is a problem with the cipher that generates the
   *     random data.
   */
  private void nextBlock() throws GeneralSecurityException {
    for (int i = 0; i < BLOCKS_AT_ONCE; i++) {
      incrementCounter();
      System.arraycopy(counter, 0, counterInput, i * COUNTER_SIZE_BYTES, COUNTER_SIZE_BYTES);
    }
    final int totalBytes = COUNTER_SIZE_BYTES * BLOCKS_AT_ONCE;
    cipher.doFinal(counterInput, 0, totalBytes, currentBlock);
  }

  /** {@inheritDoc} */
  @Override
  protected final int next(final int bits) {
    lock.lock();
    int result;
    try {
      if (currentBlock.length - index < 4) {
        try {
          nextBlock();
          index = 0;
        } catch (final GeneralSecurityException ex) {
          // Should never happen.  If initialisation succeeds without exceptions
          // we should be able to proceed indefinitely without exceptions.
          throw new IllegalStateException("Failed creating next random block.", ex);
        }
      }
      result = BinaryUtils.convertBytesToInt(currentBlock, index);
      index += 4;
    } finally {
      lock.unlock();
    }
    recordEntropySpent(bits);
    return result >>> (32 - bits);
  }

  /**
   * For debugging. Should always be true.
   *
   * @return a boolean.
   */
  public boolean isSeeded() {
    return seeded;
  }

  /** {@inheritDoc} */
  @Override
  public void setSeed(final byte[] seed) {
    if (seed.length > MAX_TOTAL_SEED_LENGTH_BYTES) {
      throw new IllegalArgumentException(
          "Seed too long: maximum " + MAX_TOTAL_SEED_LENGTH_BYTES + " bytes");
    }
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
      } finally {
        lock.unlock();
      }
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    this.seed = castNonNull(this.seed);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void setSeed(@UnknownInitialization(Random.class)AesCounterRandom this,
      final long seed) {
    if (superConstructorFinished) {
      super.setSeed(seed);
    } // Otherwise ignore; it's Random.<init> calling us without a full-size seed
  }

  /** {@inheritDoc} */
  @EnsuresNonNull({"counter", "this.seed"})
  @Override
  protected void setSeedInternal(@UnknownInitialization(Random.class)AesCounterRandom this,
      final byte[] seed) {
    super.setSeedInternal(seed);

    final int seedLength = seed.length;
    if (seedLength < 16 || seedLength > MAX_TOTAL_SEED_LENGTH_BYTES) {
      throw new IllegalArgumentException(String.format(
          "Seed length is %d bytes; need 16 to %d bytes", seedLength, MAX_TOTAL_SEED_LENGTH_BYTES));
    }
    // determine how much of seed can go to key
    final int keyLength = getKeyLength(seed);
    final byte[] key = Arrays.copyOfRange(seed, 0, keyLength);
    // rest goes to counter
    counter = new byte[COUNTER_SIZE_BYTES];
    System.arraycopy(seed, keyLength, counter, 0, seedLength - keyLength);
    assert cipher != null : "@AssumeAssertion(nullness)";
    try {
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, ALGORITHM));
    } catch (final InvalidKeyException e) {
      throw new RuntimeException("Invalid key: " + Arrays.toString(key), e);
    }
    seeded = true;
  }

  /** {@inheritDoc} */
  @Override
  public int getNewSeedLength(@UnknownInitialization AesCounterRandom this) {
    return MAX_TOTAL_SEED_LENGTH_BYTES;
  }
}
