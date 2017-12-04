package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertBytesToInt;
import static io.github.pr0methean.betterrandom.util.Byte16ArrayArithmetic.addInto;
import static io.github.pr0methean.betterrandom.util.Byte16ArrayArithmetic.copyInto;
import static io.github.pr0methean.betterrandom.util.Byte16ArrayArithmetic.makeByteArrayThreadLocal;
import static io.github.pr0methean.betterrandom.util.Byte16ArrayArithmetic.multiplyInto;
import static io.github.pr0methean.betterrandom.util.Byte16ArrayArithmetic.unsignedShiftRight;
import static io.github.pr0methean.betterrandom.util.Byte16ArrayArithmetic.xorInto;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.SeekableRandom;
import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.Byte16ArrayArithmetic;
import io.github.pr0methean.betterrandom.util.EntryPoint;

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
  public static final int ROTATION1 = Long.SIZE * 2 - WANTED_OP_BITS;
  private static final int ROTATION2 = (Long.SIZE - WANTED_OP_BITS);
  private static final int MASK = (1 << WANTED_OP_BITS) - 1;

  private static final ThreadLocal<byte[]> result = makeByteArrayThreadLocal();
  private static final ThreadLocal<byte[]> rot = makeByteArrayThreadLocal();
  private static final ThreadLocal<byte[]> shiftedOldSeed = makeByteArrayThreadLocal();
  private static final ThreadLocal<byte[]> resultTerm1 = makeByteArrayThreadLocal();
  private static final ThreadLocal<byte[]> resultTerm2 = makeByteArrayThreadLocal();
  private static final ThreadLocal<byte[]> curMult = makeByteArrayThreadLocal();
  private static final ThreadLocal<byte[]> curPlus = makeByteArrayThreadLocal();
  private static final ThreadLocal<byte[]> accMult = makeByteArrayThreadLocal();
  private static final ThreadLocal<byte[]> accPlus = makeByteArrayThreadLocal();
  private static final ThreadLocal<byte[]> adjMult = makeByteArrayThreadLocal();
  private ThreadLocal<byte[]> shifted = makeByteArrayThreadLocal();
  private ThreadLocal<byte[]> rshift = makeByteArrayThreadLocal();

  public Pcg128Random() {
    this(DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  }

  @EntryPoint public Pcg128Random(SeedGenerator seedGenerator) throws SeedException {
    this(seedGenerator.generateSeed(SEED_SIZE_BYTES));
  }

  @EntryPoint public Pcg128Random(byte[] seed) {
    super(seed);
    if (seed.length != SEED_SIZE_BYTES) {
      throw new IllegalArgumentException("Pcg64Random requires a 16-byte seed");
    }
  }

  @SuppressWarnings("NonSynchronizedMethodOverridesSynchronizedMethod") @Override
  public void setSeed(long seed) {
    fallbackSetSeedIfInitialized();
  }

  // TODO: convert to 128 bits
  @Override public void advance(long delta) {
    if (delta == 0) {
      return;
    }
    // The method used here is based on Brown, "Random Number Generation
    // with Arbitrary Stride,", Transactions of the American Nuclear
    // Society (Nov. 1994).  The algorithm is very similar to fast
    // exponentiation.
    byte[] curMult = Byte16ArrayArithmetic.copyInto(this.curMult, MULTIPLIER);
    byte[] curPlus = Byte16ArrayArithmetic.copyInto(this.curPlus, INCREMENT);
    byte[] accMult = Byte16ArrayArithmetic.copyInto(this.accMult, Byte16ArrayArithmetic.ONE);
    byte[] accPlus = Byte16ArrayArithmetic.copyInto(this.accPlus, Byte16ArrayArithmetic.ZERO);
    while (delta != 0) {
      if ((delta & 1) == 1) {
        multiplyInto(accMult, curMult);
        multiplyInto(accPlus, curMult);
        addInto(accPlus, curPlus);
      }
      byte[] adjMult = Byte16ArrayArithmetic.copyInto(this.adjMult, curMult);
      addInto(adjMult, 1);
      multiplyInto(curPlus, adjMult);
      multiplyInto(curMult, curMult);
      delta >>>= 1;
    }
    lock.lock();
    try {
      multiplyInto(seed, accMult);
      addInto(seed, accPlus);
    } finally {
      lock.unlock();
    }
  }

  @Override public void setSeedInternal(byte[] seed) {
    if (seed.length != SEED_SIZE_BYTES) {
      throw new IllegalArgumentException("Pcg128Random requires a 16-byte seed");
    }
    super.setSeedInternal(seed);
  }

  @Override protected int next(int bits) {
    byte[] result = internalNext();
    return convertBytesToInt(result, SEED_SIZE_BYTES - Integer.BYTES);
  }

  @Override protected long nextLongNoEntropyDebit() {
    byte[] result = internalNext();
    return BinaryUtils.convertBytesToLong(result, SEED_SIZE_BYTES - Long.BYTES);
  }

  private byte[] internalNext() {
    lock.lock();
    byte[] rot;
    byte[] result;
    byte[] shiftedOldSeed;
    int rshift_int;
    byte[] shifted;
    final int mask = (1 << WANTED_OP_BITS) - 1;
    final int xshift     = WANTED_OP_BITS + (Long.SIZE + mask)/2;

    try {
      rot = Byte16ArrayArithmetic.copyInto(this.rot, seed);
      shiftedOldSeed = copyInto(this.shiftedOldSeed, seed);
      unsignedShiftRight(shiftedOldSeed, ROTATION1);
      rshift_int = convertBytesToInt(shiftedOldSeed, 12) & MASK;
      shifted = copyInto(this.shifted, seed);
      unsignedShiftRight(shifted, xshift);
      xorInto(seed, shifted);
      result = Byte16ArrayArithmetic.copyInto(this.result, seed);
    } finally {
      lock.unlock();
    }
    unsignedShiftRight(rot, (Long.SIZE - WANTED_OP_BITS - MASK +
        convertBytesToInt(shiftedOldSeed, SEED_SIZE_BYTES - Integer.BYTES)));
    unsignedShiftRight(result, ROTATION2);
    final int ampRot = convertBytesToInt(rot, SEED_SIZE_BYTES - Integer.BYTES);
    byte[] resultTerm1 = Byte16ArrayArithmetic.copyInto(this.resultTerm1, result);
    unsignedShiftRight(resultTerm1, ampRot);
    byte[] resultTerm2 = Byte16ArrayArithmetic.copyInto(this.resultTerm2, result);
    Byte16ArrayArithmetic.unsignedShiftLeft(resultTerm2, Integer.SIZE - ampRot);
    addInto(resultTerm2, resultTerm1);
    return resultTerm2;
  }

  @Override protected ToStringHelper addSubclassFields(ToStringHelper original) {
    return original;
  }

  @Override public int getNewSeedLength() {
    return SEED_SIZE_BYTES;
  }
}
