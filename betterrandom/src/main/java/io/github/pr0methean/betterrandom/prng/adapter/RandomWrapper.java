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
package io.github.pr0methean.betterrandom.prng.adapter;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import io.github.pr0methean.betterrandom.EntropyCountingRandom;
import io.github.pr0methean.betterrandom.RepeatableRandom;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.Dumpable;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.security.SecureRandom;
import java.util.Random;
import javax.annotation.Nullable;

/**
 * <p>Wraps any {@link Random} as a {@link RepeatableRandom} and {@link ByteArrayReseedableRandom}.
 * Can be used to encapsulate away a change of implementation in midstream.</p>
 * <p>Caution: This depends on the delegate's thread-safety. When used with a vanilla {@link
 * Random}, this means that its output for the same seed will vary when accessed concurrently
 * from multiple threads, at least on JDK 7 and 8, if the calls include e.g. {@link #nextLong()},
 * {@link #nextGaussian()} or {@link #nextDouble()}. However, {@link #nextInt()} will still be
 * transactional.</p>
 *
 * @author Chris Hennick
 */
public class RandomWrapper<T extends Random> extends BaseRandom {

  private static final byte[] DUMMY_SEED = new byte[8];
  private static final long serialVersionUID = -6526304552538799385L;
  private volatile T wrapped;
  private volatile boolean unknownSeed;

  /**
   * Creates an instance wrapping the given {@link Random}.
   *
   * @param wrapped The {@link Random} to wrap.
   */
  @EntryPoint public RandomWrapper(final T wrapped) {
    super(getSeedOrDummy(wrapped)); // We won't know the wrapped PRNG's seed
    unknownSeed = !(wrapped instanceof RepeatableRandom);
    readEntropyOfWrapped(wrapped);
    this.wrapped = wrapped;
  }

  private static byte[] getSeedOrDummy(final Random wrapped) {
    return (wrapped instanceof RepeatableRandom) ? ((RepeatableRandom) wrapped).getSeed() :
        DUMMY_SEED;
  }

  /**
   * Creates an instance wrapping a basic {@link Random}.
   *
   * @param seed the seed
   * @return an instance
   */
  public static RandomWrapper<Random> wrapJavaUtilRandom(final long seed) {
    RandomWrapper<Random> wrapper = new RandomWrapper<>(new Random(seed));
    wrapper.setInitiallyKnownSeed(BinaryUtils.convertLongToBytes(seed));
    return wrapper;
  }

  /**
   * Creates an instance wrapping a basic {@link Random}.
   *
   * @param seed the seed; must be 8 bytes
   * @return an instance
   */
  public static RandomWrapper<Random> wrapJavaUtilRandom(final byte[] seed) {
    RandomWrapper<Random> wrapper = new RandomWrapper<>(new Random(BinaryUtils.convertBytesToLong(checkLength(seed, Long.BYTES))));
    wrapper.setInitiallyKnownSeed(seed);
    return wrapper;
  }

  public static RandomWrapper<Random> wrapJavaUtilRandom(
      final SeedGenerator seedGenerator) throws SeedException {
    return wrapJavaUtilRandom(seedGenerator.generateSeed(Long.BYTES));
  }

  protected void setInitiallyKnownSeed(byte[] bytes) {
    System.arraycopy(bytes, 0, seed, 0, Long.BYTES);
    entropyBits.set(bytes.length * 8L);
    unknownSeed = false;
  }

  @Override public String toString() {
    return String.format("RandomWrapper (currently around %s)", wrapped);
  }

  @Override public boolean usesParallelStreams() {
    return true; // Streams should be parallel, in case a parallel PRNG is switched in later
  }

  @Override protected int next(final int bits) {
    return (bits >= 32) ? getWrapped().nextInt() :
        (bits == 31) ? getWrapped().nextInt() >>> 1 : getWrapped().nextInt(1 << bits);
  }

  /**
   * Returns the PRNG this RandomWrapper is currently wrapping.
   *
   * @return the wrapped {@link Random} instance
   */
  @EntryPoint public T getWrapped() {
    lock.lock();
    try {
      return wrapped;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Replaces the wrapped PRNG with the given one on subsequent calls.
   *
   * @param wrapped an {@link Random} instance to wrap
   */
  @EntryPoint public void setWrapped(final T wrapped) {
    lock.lock();
    try {
      this.wrapped = wrapped;
      readEntropyOfWrapped(wrapped);
      seed = getSeedOrDummy(wrapped);
      unknownSeed = !(wrapped instanceof RepeatableRandom);
    } finally {
      lock.unlock();
    }
  }

  private void readEntropyOfWrapped(final Random wrapped) {
    entropyBits.set((wrapped instanceof EntropyCountingRandom) ?
        ((EntropyCountingRandom) wrapped).getEntropyBits() :
        ((wrapped instanceof RepeatableRandom) ?
            (((RepeatableRandom) wrapped).getSeed().length * (long) (Byte.SIZE)) : Long.SIZE));
  }

  @Override protected ToStringHelper addSubclassFields(final ToStringHelper original) {
    Random currentWrapped = wrapped;
    return original
        .add("wrapped", currentWrapped instanceof Dumpable ? ((Dumpable) currentWrapped).dump()
            : currentWrapped.toString());
  }

  /**
   * Returns the wrapped PRNG's seed, if we know it. When this RandomWrapper is wrapping a passed-in
   * {@link Random} that's not a {@link RepeatableRandom}, we won't know the seed until the next
   * {@link #setSeed(byte[])} or {@link #setSeed(long)} call lets us set it ourselves, and so an
   * {@link UnsupportedOperationException} will be thrown until then.
   *
   * @throws UnsupportedOperationException if this RandomWrapper doesn't know the wrapped PRNG's
   *     seed.
   */
  @Override public byte[] getSeed() {
    lock.lock();
    try {
      if (unknownSeed) {
        throw new UnsupportedOperationException();
      }
      return wrapped instanceof RepeatableRandom ? ((RepeatableRandom) wrapped).getSeed() : seed.clone();
    } finally {
      lock.unlock();
    }
  }

  @SuppressWarnings("LockAcquiredButNotSafelyReleased") @Override
  public void setSeed(final long seed) {
    boolean locked = false;
    if (lock != null) {
      lock.lock();
      locked = true;
    }
    try {
      if (wrapped != null) {
        wrapped.setSeed(seed);
        super.setSeedInternal(BinaryUtils.convertLongToBytes(seed));
        unknownSeed = false;
      }
    } finally {
      if (locked) {
        lock.unlock();
      }
    }
  }

  /**
   * Delegates to one of {@link ByteArrayReseedableRandom#setSeed(byte[])}, {@link
   * SecureRandom#setSeed(byte[])} or {@link Random#setSeed(long)}.
   *
   * @param seed The new seed.
   */
  @SuppressWarnings("LockAcquiredButNotSafelyReleased") @Override protected void setSeedInternal(
      final byte[] seed) {
    if (seed == null) {
      throw new IllegalArgumentException("Seed must not be null");
    }
    boolean locked = false;
    if (lock != null) {
      lock.lock();
      locked = true;
    }
    try {
      if ((this.seed == null) || (this.seed.length != seed.length)) {
        this.seed = new byte[seed.length];
      }
      super.setSeedInternal(seed);
      if (wrapped == null) {
        return;
      }
      @Nullable ByteArrayReseedableRandom asByteArrayReseedable = null;
      if (wrapped instanceof ByteArrayReseedableRandom) {
        asByteArrayReseedable = (ByteArrayReseedableRandom) wrapped;
        if (asByteArrayReseedable.preferSeedWithLong() && (seed.length == Long.BYTES)) {
          asByteArrayReseedable = null;
        }
      } else if (wrapped instanceof SecureRandom) {
        // Special handling, since SecureRandom isn't ByteArrayReseedableRandom but does have
        // setSeed(byte[])
        ((SecureRandom) wrapped).setSeed(seed);
        unknownSeed = false;
        return;
      } else {
        checkLength(seed, Long.BYTES);
      }
      if (asByteArrayReseedable != null) {
        asByteArrayReseedable.setSeed(seed);
      } else {
        wrapped.setSeed(BinaryUtils.convertBytesToLong(seed));
      }
      unknownSeed = false;
    } finally {
      if (locked) {
        lock.unlock();
      }
    }
  }

  @Override protected boolean supportsMultipleSeedLengths() {
    return true; // Seed-length checking can be done by wrapped
  }

  @Override public boolean preferSeedWithLong() {
    if (lock == null) {
      return false; // safe default
    }
    lock.lock();
    try {
      final Random currentWrapped = getWrapped();
      return !(currentWrapped instanceof ByteArrayReseedableRandom) ||
          ((ByteArrayReseedableRandom) currentWrapped).preferSeedWithLong();
    } finally {
      lock.unlock();
    }
  }

  @Override public int getNewSeedLength() {
    if (lock == null) {
      return 0; // can't use a seed yet
    }
    lock.lock();
    try {
      if (wrapped == null) {
        return 0;
      }
      return (wrapped instanceof ByteArrayReseedableRandom) ?
          ((ByteArrayReseedableRandom) wrapped).getNewSeedLength() : Long.BYTES;
    } finally {
      lock.unlock();
    }
  }

  @Override public void nextBytes(final byte[] bytes) {
    debitEntropy(bytes.length * (long) (Byte.SIZE));
    getWrapped().nextBytes(bytes);
  }

  @Override public int nextInt() {
    debitEntropy(Integer.SIZE);
    return getWrapped().nextInt();
  }

  @Override public int nextInt(final int bound) {
    debitEntropy(entropyOfInt(0, bound));
    return getWrapped().nextInt(bound);
  }

  @Override protected long nextLongNoEntropyDebit() {
    return getWrapped().nextLong();
  }

  @Override public boolean nextBoolean() {
    debitEntropy(1);
    return getWrapped().nextBoolean();
  }

  @Override public float nextFloat() {
    debitEntropy(ENTROPY_OF_FLOAT);
    return getWrapped().nextFloat();
  }

  @Override public double nextDoubleNoEntropyDebit() {
    return getWrapped().nextDouble();
  }

  @Override public double nextGaussian() {
    // Upper bound. 2 Gaussians are generated from 2 nextDouble calls, and at least 1 of the 2 is
    // output.
    debitEntropy(ENTROPY_OF_DOUBLE);
    return getWrapped().nextGaussian();
  }
}
