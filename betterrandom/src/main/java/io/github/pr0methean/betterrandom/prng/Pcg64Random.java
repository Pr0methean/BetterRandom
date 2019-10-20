package io.github.pr0methean.betterrandom.prng;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.SeekableRandom;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>From the original description, "PCG is a family of simple fast space-efficient statistically
 * good algorithms for random number generation. Unlike many general-purpose RNGs, they are also
 * hard to predict." This is a Java port of the "XSH RR 64/32" generator presented at <a
 * href="http://www.pcg-random.org/">http://www.pcg-random.org/</a>. Period is 2<sup>62</sup> bits.
 * This PRNG is seekable.
 * </p><p>
 * Sharing a single instance across threads that are frequently using it concurrently isn't
 * recommended, unless memory is too constrained to use with a
 * {@link io.github.pr0methean.betterrandom.prng.concurrent.ThreadLocalRandomWrapper}.
 * </p>
 *
 * @author M.E. O'Neill (algorithm and C++ implementation)
 * @author Chris Hennick (Java port)
 */
public class Pcg64Random extends BaseRandom implements SeekableRandom {

  private static final long serialVersionUID = 1677405697790847137L;
  private static final long MULTIPLIER = 6364136223846793005L;
  private static final long INCREMENT = 1442695040888963407L;
  private static final int WANTED_OP_BITS = 5;
  private static final int ROTATION1 = (WANTED_OP_BITS + Integer.SIZE) / 2;
  private static final int ROTATION2 = (Long.SIZE - Integer.SIZE - WANTED_OP_BITS);
  private static final int ROTATION3 = Long.SIZE - WANTED_OP_BITS;
  private static final int MASK = (1 << WANTED_OP_BITS) - 1;

  private final AtomicLong internal;

  public Pcg64Random() {
    this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  }

  @EntryPoint public Pcg64Random(final SeedGenerator seedGenerator) throws SeedException {
    this(seedGenerator.generateSeed(Long.BYTES));
  }

  @EntryPoint public Pcg64Random(final byte[] seed) {
    super(seed);
    if (seed.length != Long.BYTES) {
      throw new IllegalArgumentException("Pcg64Random requires an 8-byte seed");
    }
    internal = new AtomicLong(BinaryUtils.convertBytesToLong(seed));
  }

  @EntryPoint public Pcg64Random(final long seed) {
    super(seed);
    internal = new AtomicLong(seed);
  }

  @Override protected long nextLongNoEntropyDebit() {
    lock.lock();
    try {
      return ((long) (next(32)) << 32) + next(32);
    } finally {
      lock.unlock();
    }
  }

  @Override public byte[] getSeed() {
    return BinaryUtils.convertLongToBytes(internal.get()).clone();
  }

  @Override
  public void setSeed(final long seed) {
    if (internal != null) {
      lock.lock();
      try {
        internal.set(seed);
        creditEntropyForNewSeed(Long.BYTES);
      } finally {
        lock.unlock();
      }
    }
  }

  @Override public void advance(long delta) {
    if (delta == 0) {
      return;
    }
    // The method used here is based on Brown, "Random Number Generation
    // with Arbitrary Stride,", Transactions of the American Nuclear
    // Society (Nov. 1994).  The algorithm is very similar to fast
    // exponentiation.
    long curMult = MULTIPLIER;
    long curPlus = INCREMENT;
    long accMult = 1;
    long accPlus = 0;
    while (delta != 0) {
      if ((delta & 1) == 1) {
        accMult *= curMult;
        accPlus = (accPlus * curMult) + curPlus;
      }
      curPlus = (curMult + 1) * curPlus;
      curMult *= curMult;
      delta >>>= 1;
    }
    final long finalAccMult = accMult;
    final long finalAccPlus = accPlus;
    lock.lock();
    try {
      internal.updateAndGet(old -> (finalAccMult * old) + finalAccPlus);
    } finally {
      lock.unlock();
    }
  }

  @Override public void setSeedInternal(final byte[] seed) {
    super.setSeedInternal(seed);
    if (internal != null) {
      lock.lock();
      try {
        internal.set(BinaryUtils.convertBytesToLong(seed));
      } finally {
        lock.unlock();
      }
    }
  }

  @SuppressWarnings("NumericCastThatLosesPrecision") @Override protected int next(final int bits) {
    long oldInternal;
    long newInternal;
    do {
      oldInternal = internal.get();
      newInternal = (oldInternal * MULTIPLIER) + INCREMENT;
    } while (!internal.compareAndSet(oldInternal, newInternal));
    // Calculate output function (XSH RR), uses old state for max ILP
    final int xorshifted = (int) (((oldInternal >>> ROTATION1) ^ oldInternal) >>> ROTATION2);
    final int rot = (int) (oldInternal >>> ROTATION3);
    return ((xorshifted >>> rot) | (xorshifted << ((-rot) & MASK))) >>> (Integer.SIZE - bits);
  }

  @Override protected ToStringHelper addSubclassFields(final ToStringHelper original) {
    return original.add("internal", internal.get());
  }

  @Override public int getNewSeedLength() {
    return Long.BYTES;
  }

  private void writeObject(final ObjectOutputStream out) throws IOException {
    // Copy the long seed back to the array seed
    BinaryUtils.convertLongToBytes(internal.get(), seed, 0);
    out.defaultWriteObject();
  }
}
