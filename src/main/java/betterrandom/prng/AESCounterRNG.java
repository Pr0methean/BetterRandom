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
package betterrandom.prng;

import betterrandom.seed.DefaultSeedGenerator;
import betterrandom.seed.SeedException;
import betterrandom.seed.SeedGenerator;
import betterrandom.util.BinaryUtils;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * <p>Non-linear random number generator based on the AES block cipher in counter mode. Uses the
 * seed as a key to encrypt a 128-bit counter using AES(Rijndael).</p>
 *
 * <p>By default, we only use a 128-bit key for the cipher because any larger key requires the
 * inconvenience of installing the unlimited strength cryptography policy files for the Java
 * platform.  Larger keys may be used (192 or 256 bits) but if the cryptography policy files are not
 * installed, a {@link GeneralSecurityException} will be thrown.</p>
 *
 * <p><em>NOTE: Because instances of this class require 128-bit seeds, it is not possible to seed
 * this RNG using the {@link #setSeed(long)} method inherited from {@link Random}.  Calls to this
 * method will have no effect. Instead the seed must be set by a constructor.</em></p>
 *
 * <p><em>NOTE: THIS CLASS IS NOT SERIALIZABLE</em></p>
 *
 * @author Daniel Dyer
 */
public class AESCounterRNG extends BaseRNG implements RepeatableRNG {

  private static final long serialVersionUID = 5949778642428995210L;

  private static final Logger LOG = Logger.getLogger(AESCounterRNG.class.getName());

  private static final int DEFAULT_SEED_SIZE_BYTES = 32;

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

  /**
   * If the seed is longer than this, part of it becomes the counter's initial value. Otherwise, the
   * full seed becomes the AES key and the counter is initially zero. Package-visible for testing of
   * its initialization.
   */
  static final int MAX_KEY_LENGTH_BYTES;
  
  static {
    try {
      MAX_KEY_LENGTH_BYTES = Math.min(Cipher.getMaxAllowedKeyLength("AES/ECB/NoPadding"), 32);
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }
  
  static final int MAX_TOTAL_SEED_LENGTH_BYTES = MAX_KEY_LENGTH_BYTES + COUNTER_SIZE_BYTES;

  private transient Cipher cipher;
  private final byte[] counter = new byte[COUNTER_SIZE_BYTES];
  private boolean counterInitialized = false;
  private transient byte[] counterInput;
  private boolean seeded = false;

  // Ignore setSeed calls from super constructor
  private transient boolean superConstructorFinished = false;
  private int entropyBytes;

  /**
   * Called in constructor and readObject to initialize transient fields.
   */
  @Override
  protected void initTransientFields() {
    LOG.fine("initTransientFields starting");
    super.initTransientFields();
    counterInput = new byte[COUNTER_SIZE_BYTES * BLOCKS_AT_ONCE];
    try {
      cipher = Cipher.getInstance("AES/ECB/NoPadding");
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      throw new RuntimeException("JVM doesn't provide AES/ECB/NoPadding", e);
    }
    setSeed(seed);
    LOG.fine("initTransientFields done");
  }

  private final byte[] currentBlock = new byte[COUNTER_SIZE_BYTES * BLOCKS_AT_ONCE];

  // force generation of first block on demand
  private int index = currentBlock.length;


  /**
   * Creates a new RNG and seeds it using 256 bits from the default seeding strategy.
   *
   * @throws GeneralSecurityException If there is a problem initialising the AES cipher.
   */
  public AESCounterRNG() throws GeneralSecurityException {
    this(DEFAULT_SEED_SIZE_BYTES);
  }


  /**
   * Seed the RNG using the provided seed generation strategy to create a 128-bit seed.
   *
   * @param seedGenerator The seed generation strategy that will provide the seed value for this
   * RNG.
   * @throws SeedException If there is a problem generating a seed.
   * @throws GeneralSecurityException If there is a problem initialising the AES cipher.
   */
  public AESCounterRNG(SeedGenerator seedGenerator) throws SeedException,
      GeneralSecurityException {
    this(seedGenerator.generateSeed(DEFAULT_SEED_SIZE_BYTES));
  }


  /**
   * Seed the RNG using the default seed generation strategy to create a seed of the specified
   * size.
   *
   * @param seedSizeBytes The number of bytes to use for seed data.  Valid values are 16 (128 bits),
   * 24 (192 bits) and 32 (256 bits).  Any other values will result in an exception from the AES
   * implementation.
   * @throws GeneralSecurityException If there is a problem initialising the AES cipher.
   * @since 1.0.2
   */
  public AESCounterRNG(int seedSizeBytes) throws GeneralSecurityException {
    this(DefaultSeedGenerator.getInstance().generateSeed(seedSizeBytes));
  }


  /**
   * Creates an RNG and seeds it with the specified seed data.
   *
   * @param seed The seed data used to initialise the RNG.
   */
  public AESCounterRNG(byte[] seed) {
    super(seed);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] getSeed() {
    lock.lock();
    try {
      return seed.clone();
    } finally {
      lock.unlock();
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
   * random data.
   */
  private void nextBlock() throws GeneralSecurityException {
    for (int i = 0; i < BLOCKS_AT_ONCE; i++) {
      incrementCounter();
      System.arraycopy(counter, 0, counterInput, i * COUNTER_SIZE_BYTES, COUNTER_SIZE_BYTES);
    }
    int totalBytes = COUNTER_SIZE_BYTES * BLOCKS_AT_ONCE;
    System.arraycopy(cipher.doFinal(counterInput), 0, currentBlock, 0,
        totalBytes);
    entropyBytes -= totalBytes;
  }

  /**
   * {@inheritDoc}
   *
   * The given seed is combined with the existing seed. Thus, this method is guaranteed not to
   * reduce randomness.
   */
  @Override
  public void setSeed(long seed) {
    if (!superConstructorFinished) {
      // setSeed is called by super() but won't work yet
      return;
    }
    super.setSeed(seed);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected final int next(int bits) {
    int result;
    lock.lock();
    try {
      if (currentBlock.length - index < 4) {
        try {
          nextBlock();
          index = 0;
        } catch (GeneralSecurityException ex) {
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
    return result >>> (32 - bits);
  }

  @SuppressWarnings("NonFinalFieldReferenceInEquals")
  @Override
  public boolean equals(Object other) {
    return other instanceof AESCounterRNG
        && Arrays.equals(seed, ((AESCounterRNG) other).seed);
  }

  @SuppressWarnings("NonFinalFieldReferencedInHashCode")
  @Override
  public int hashCode() {
    return Arrays.hashCode(seed);
  }

  @Override
  public void setSeed(byte[] seed) {
    if (!superConstructorFinished) {
      LOG.fine("Skipping setSeed: still in super constructor");
      // setSeed can't work until seed array allocated
      return;
    }
    if (seed.length > MAX_TOTAL_SEED_LENGTH_BYTES) {
      throw new IllegalArgumentException("Seed too long: maximum " + MAX_TOTAL_SEED_LENGTH_BYTES + " bytes");
    }
    lock.lock();
    try {
      byte[] key;
      if (seed.length == MAX_KEY_LENGTH_BYTES) {
        LOG.fine("setSeed: using full seed as key");
        key = seed;
      } else {
        if (!seeded) {
          if (seed.length < 16) {
            throw new IllegalArgumentException("Seed too short: need at least 16 bytes");
          } else {
            if (seed.length > MAX_KEY_LENGTH_BYTES) {
              // part of the seed goes to key; rest goes to counter
              key = Arrays.copyOfRange(seed, 0, seed.length - COUNTER_SIZE_BYTES);
        
              // copy to counter only if counter hasn't already been deserialized
              if (!counterInitialized) {
                System.arraycopy(seed, seed.length - COUNTER_SIZE_BYTES, counter, 0, COUNTER_SIZE_BYTES);
                counterInitialized = true;
              }
            } else {
              key = seed;
            }
          }
          this.seed = seed;
        } else {
          // Extend the key
          byte[] newSeed = new byte[this.seed.length + seed.length];
          System.arraycopy(this.seed, 0, newSeed, 0, this.seed.length);
          System.arraycopy(seed, 0, newSeed, this.seed.length, seed.length);
          if (newSeed.length > MAX_KEY_LENGTH_BYTES) {
            int keyBytes = newSeed.length - COUNTER_SIZE_BYTES;
            key = new byte[keyBytes];
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(newSeed);
            System.arraycopy(md.digest(), 0, key, 0, keyBytes);
          } else {
            key = newSeed;
          }
          // copy to counter only if counter hasn't already been deserialized
          if (!counterInitialized) {
            System.arraycopy(seed, seed.length - COUNTER_SIZE_BYTES, counter, 0, COUNTER_SIZE_BYTES);
            counterInitialized = true;
          }
          this.seed = newSeed;
        }
      }
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
      entropyBytes = Math.max(seed.length + entropyBytes,
          MAX_TOTAL_SEED_LENGTH_BYTES);
      seeded = true;
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new RuntimeException(e);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public int getNewSeedLength() {
    return MAX_KEY_LENGTH_BYTES;
  }

  @Override
  public int entropyOctets() {
    return entropyBytes;
  }
}
/*
    lock.lock();
    try {
      super.setSeed(seed);
      byte[] key;
      if (seed.length > MAX_KEY_LENGTH_BYTES) {
        // part of the seed goes to key; rest goes to counter
        key = Arrays.copyOfRange(seed, 0, seed.length - COUNTER_SIZE_BYTES);
  
        // copy to counter only if counter hasn't already been deserialized
        if (!counterInitialized) {
          System.arraycopy(seed, seed.length - COUNTER_SIZE_BYTES, counter, 0, COUNTER_SIZE_BYTES);
          counterInitialized = true;
        }
      } else {
        key = seed;
      }
      try {
        cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
      } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }
      counterInput = new byte[COUNTER_SIZE_BYTES * BLOCKS_AT_ONCE];
      entropyBytes = seed.length;
    } finally {
      lock.unlock();
    }
    */