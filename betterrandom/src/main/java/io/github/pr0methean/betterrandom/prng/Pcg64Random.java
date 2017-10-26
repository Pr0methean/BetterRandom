package io.github.pr0methean.betterrandom.prng;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>From the original description, "PCG is a family of simple fast space-efficient statistically
 * good algorithms for random number generation. Unlike many general-purpose RNGs, they are also
 * hard to predict." This is a Java port of the "XSH RR 64/32" generator presented at <a
 * href="http://www.pcg-random.org/">http://www.pcg-random.org/</a>. Period is 2<sup>62</sup> bits.
 * This PRNG is seekable.
 * </p><p>
 * Concurrency is lockless, but contention is still possible due to a retry loop of {@link
 * AtomicLong#compareAndSet(long, long)}. Thus, sharing a single instance across threads isn't
 * recommended unless memory is too constrained to use with a {@link ThreadLocalRandomWrapper}.
 * </p>
 *
 * @author M.E. O'Neill (algorithm and C++ implementation)
 * @author Chris Hennick (Java port)
 */
public class Pcg64Random extends BaseRandom
    implements io.github.pr0methean.betterrandom.SeekableRandom {

  private static final long serialVersionUID = 1677405697790847137L;
  private static final long MULTIPLIER = 6364136223846793005L;
  private static final long INCREMENT = 1442695040888963407L;
  private static final int WANTED_OP_BITS = 5;
  private static final int ROTATION1 = (WANTED_OP_BITS + Integer.SIZE) / 2;
  private static final int ROTATION2 = (Long.SIZE - Integer.SIZE - WANTED_OP_BITS);
  private static final int MASK = (1 << WANTED_OP_BITS) - 1;

  private AtomicLong internal;

  public Pcg64Random() {
    this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  }

  @Override public byte[] getSeed() {
    return BinaryUtils.convertLongToBytes(internal.get());
  }

  @EntryPoint public Pcg64Random(SeedGenerator seedGenerator) throws SeedException {
    this(seedGenerator.generateSeed(Long.BYTES));
  }

  @EntryPoint public Pcg64Random(byte[] seed) {
    super(seed);
    if (seed.length != Long.BYTES) {
      throw new IllegalArgumentException("Pcg64Random requires an 8-byte seed");
    }
    internal = new AtomicLong(BinaryUtils.convertBytesToLong(seed));
  }

  @EntryPoint public Pcg64Random(long seed) {
    super(seed);
    internal = new AtomicLong(seed);
  }

  @Override public void advance(long delta) {
    // The method used here is based on Brown, "Random Number Generation
    // with Arbitrary Stride,", Transactions of the American Nuclear
    // Society (Nov. 1994).  The algorithm is very similar to fast
    // exponentiation.
    //
    // Even though delta is an unsigned integer, we can pass a
    // signed integer to go backwards, it just goes "the long way round".
    long cur_mult = MULTIPLIER;
    long cur_plus = INCREMENT;
    long acc_mult = 1;
    long acc_plus = 0;
    while (delta > 0) {
      if ((delta & 1) == 1) {
        acc_mult *= cur_mult;
        acc_plus = (acc_plus * cur_mult) + cur_plus;
      }
      cur_plus = (cur_mult + 1) * cur_plus;
      cur_mult *= cur_mult;
      delta >>= 1;
    }
    long finalAcc_mult = acc_mult;
    long finalAcc_plus = acc_plus;
    internal.updateAndGet(old -> (finalAcc_mult * old) + finalAcc_plus);
  }

  @SuppressWarnings("NonSynchronizedMethodOverridesSynchronizedMethod")
  @Override public void setSeed(long seed) {
    super.setSeed(seed);
    if (internal != null) {
      internal.set(seed);
    }
  }

  @Override public void setSeed(byte[] seed) {
    super.setSeed(seed);
    if (seed.length != Long.BYTES) {
      throw new IllegalArgumentException("Pcg64Random requires an 8-byte seed");
    }
    internal.set(BinaryUtils.convertBytesToLong(seed));
  }

  @Override protected int next(int bits) {
    long oldInternal;
    long newInternal;
    do {
      oldInternal = internal.updateAndGet(old -> (MULTIPLIER * old) + INCREMENT);
      newInternal = oldInternal;
      newInternal ^= oldInternal >> ROTATION1;
    } while (!internal.compareAndSet(oldInternal, newInternal));
    int rot = (int) (oldInternal >> (Long.SIZE - WANTED_OP_BITS)) & MASK;
    int result = (int) (newInternal >> ROTATION2);
    final int ampRot = rot & MASK;
    result >>= (ampRot + (result << (Integer.SIZE - ampRot)));
    if (bits < 32) {
      result &= (1 << bits) - 1;
    }
    return result;
  }

  @Override protected ToStringHelper addSubclassFields(ToStringHelper original) {
    return original.add("internal", internal.get());
  }

  @Override public int getNewSeedLength() {
    return Long.BYTES;
  }
}
