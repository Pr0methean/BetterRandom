package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.util.Byte16ArrayArithmetic.addInto;
import static io.github.pr0methean.betterrandom.util.Byte16ArrayArithmetic.multiplyInto;
import static io.github.pr0methean.betterrandom.util.Byte16ArrayArithmetic.unsignedShiftRight;
import static io.github.pr0methean.betterrandom.util.Byte16ArrayArithmetic.unsignedShiftRightLeast64;
import static io.github.pr0methean.betterrandom.util.Byte16ArrayArithmetic.xorInto;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.SeekableRandom;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.Byte16ArrayArithmetic;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>From the original description, "PCG is a family of simple fast space-efficient statistically
 * good algorithms for random number generation. Unlike many general-purpose RNGs, they are also
 * hard to predict." This is a Java port of the "XSH RR 128/64" generator presented at <a
 * href="http://www.pcg-random.org/">http://www.pcg-random.org/</a>. Period is 2<sup>126</sup> bits.
 * This PRNG is seekable.
 * </p><p>
 * Sharing a single instance across threads that are frequently using it concurrently isn't
 * recommended unless memory is too constrained to use with a {@link ThreadLocalRandomWrapper}.
 * </p>
 * @author M.E. O'Neill (algorithm and C++ implementation)
 * @author Chris Hennick (Java port)
 */
public class Pcg128Random extends BaseRandom implements SeekableRandom {

  private static final int SEED_SIZE_BYTES = 2 * Long.BYTES;
  private static final byte[] MULTIPLIER =
      {0x00000023, 0x00000060, 0xffffffed, 0x00000005, 0x0000001f, 0xffffffc6, 0x0000005d,
          0xffffffa4, 0x00000043, 0xffffff85, 0xffffffdf, 0x00000064, 0xffffff9f, 0xffffffcc,
          0xfffffff6, 0x00000045};
  private static final byte[] INCREMENT =
      {0x00000058, 0x00000051, 0xfffffff4, 0x0000002d, 0x0000004c, 0xffffff95, 0x0000007f,
          0x0000002d, 0x00000014, 0x00000005, 0x0000007b, 0x0000007e, 0xfffffff7, 0x00000067,
          0xffffff81, 0x0000004f};
  private static final int WANTED_OP_BITS = 6;
  public static final int ROTATION1 = (WANTED_OP_BITS + Long.SIZE) / 2;
  private static final int ROTATION2 = Long.SIZE - WANTED_OP_BITS;

  public static final double RANDOM_DOUBLE_INCR = 0x1.0p-53;
  private static final int MASK = (1 << WANTED_OP_BITS) - 1;

  private static ThreadLocal<byte[]> oldSeed = Byte16ArrayArithmetic.makeByteArrayThreadLocal();
  private static ThreadLocal<byte[]> xorShifted = Byte16ArrayArithmetic.makeByteArrayThreadLocal();
  private transient byte[] curMult;
  private transient byte[] curPlus;
  private transient byte[] accMult;
  private transient byte[] accPlus;
  private transient byte[] adjMult;
  private final Lock advancementLock = new ReentrantLock(); // guards *Mult and *Plus

  @Override protected void initTransientFields() {
    super.initTransientFields();
    curMult = new byte[SEED_SIZE_BYTES];
    curPlus = new byte[SEED_SIZE_BYTES];
    accMult = new byte[SEED_SIZE_BYTES];
    accPlus = new byte[SEED_SIZE_BYTES];
    adjMult = new byte[SEED_SIZE_BYTES];
  }

  public Pcg128Random() {
    this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  }

  @EntryPoint public Pcg128Random(SeedGenerator seedGenerator) throws SeedException {
    this(seedGenerator.generateSeed(SEED_SIZE_BYTES));
  }

  @EntryPoint public Pcg128Random(byte[] seed) {
    super(seed);
    if (seed.length != SEED_SIZE_BYTES) {
      throw new IllegalArgumentException("Pcg128Random requires a 16-byte seed");
    }
  }

  @Override public synchronized void setSeed(long seed) {
    fallbackSetSeedIfInitialized();
  }

  @Override protected double nextDoubleNoEntropyDebit() {
    return (nextLongNoEntropyDebit() >>> (Long.SIZE - ENTROPY_OF_DOUBLE)) * RANDOM_DOUBLE_INCR;
  }

  // TODO: convert to 128 bits
  @Override public void advance(long delta) {
    advance((delta < 0) ? -1 : 0, delta);
  }

  /**
   * Advances the generator forward {@code highDelta << 64 + lowDelta} steps, but does so in
   * logarithmic time.
   * @param highDelta high quadword of the distance to advance
   * @param lowDelta low quadword of the distance to advance
   */
  public void advance(long highDelta, long lowDelta) {
    advancementLock.lock();
    try {
      if (highDelta == 0 && lowDelta == 0) {
        return;
      }
      // The method used here is based on Brown, "Random Number Generation
      // with Arbitrary Stride,", Transactions of the American Nuclear
      // Society (Nov. 1994).  The algorithm is very similar to fast
      // exponentiation.
      System.arraycopy(MULTIPLIER, 0, curMult, 0, SEED_SIZE_BYTES);
      System.arraycopy(INCREMENT, 0, curPlus, 0, SEED_SIZE_BYTES);
      System.arraycopy(Byte16ArrayArithmetic.ONE, 0, accMult, 0, SEED_SIZE_BYTES);
      System.arraycopy(Byte16ArrayArithmetic.ZERO, 0, accPlus, 0, SEED_SIZE_BYTES);
      while (lowDelta != 0 || highDelta != 0) {
        if ((lowDelta & 1) == 1) {
          multiplyInto(accMult, curMult);
          multiplyInto(accPlus, curMult);
          addInto(accPlus, curPlus);
        }
        System.arraycopy(curMult, 0, adjMult, 0, SEED_SIZE_BYTES);
        addInto(adjMult, 1, true);
        multiplyInto(curPlus, adjMult);
        multiplyInto(curMult, curMult);
        lowDelta >>>= 1;
        lowDelta |= (highDelta & 1L) << 63;
        highDelta >>>= 1;
      }
      lock.lock();
      try {
        multiplyInto(seed, accMult);
        addInto(seed, accPlus);
      } finally {
        lock.unlock();
      }
    } finally {
      advancementLock.unlock();
    }
  }

  @Override public void setSeedInternal(byte[] seed) {
    if (seed.length != SEED_SIZE_BYTES) {
      throw new IllegalArgumentException("Pcg128Random requires a 16-byte seed");
    }
    super.setSeedInternal(seed);
  }

  @Override protected int next(int bits) {
    return (int) (nextLongNoEntropyDebit() >>> (Long.SIZE - bits));
  }

  @Override protected long nextLongNoEntropyDebit() {
    byte[] oldSeed = Pcg128Random.oldSeed.get();
    lock.lock();
    try {
      System.arraycopy(seed, 0, oldSeed, 0, SEED_SIZE_BYTES);
      multiplyInto(seed, MULTIPLIER);
      addInto(seed, INCREMENT);
    } finally {
      lock.unlock();
    }
    byte[] xorShifted = Pcg128Random.xorShifted.get();
    // Calculate output function (XSH RR), uses old state for max ILP
    // int xorShifted = (int) (((oldInternal >>> ROTATION1) ^ oldInternal) >>> ROTATION2);
    System.arraycopy(oldSeed, 0, xorShifted, 0, SEED_SIZE_BYTES);
    unsignedShiftRight(xorShifted, ROTATION1);
    xorInto(xorShifted, oldSeed);
    long preRotate = unsignedShiftRightLeast64(xorShifted, ROTATION2);
    // int rot = (int) (oldInternal >>> (SEED_SIZE_BYTES - WANTED_OP_BITS));
    final int rot = (oldSeed[0] >>> 2) & MASK;
    // return ((xorshifted >>> rot) | (xorshifted << ((-rot) & MASK))) >>> (Integer.SIZE - bits);
    return (preRotate >>> rot) | (preRotate << ((-rot) & MASK));
  }

  @Override protected ToStringHelper addSubclassFields(ToStringHelper original) {
    return original;
  }

  @Override public int getNewSeedLength() {
    return SEED_SIZE_BYTES;
  }
}
