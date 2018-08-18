package io.github.pr0methean.betterrandom.prng;

import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertBytesToInt;
import static io.github.pr0methean.betterrandom.util.Byte16ArrayArithmetic.addInto;
import static io.github.pr0methean.betterrandom.util.Byte16ArrayArithmetic.copyInto;
import static io.github.pr0methean.betterrandom.util.Byte16ArrayArithmetic.makeByteArrayThreadLocal;
import static io.github.pr0methean.betterrandom.util.Byte16ArrayArithmetic.multiplyInto;
import static io.github.pr0methean.betterrandom.util.Byte16ArrayArithmetic.orInto;
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
import java.nio.ByteBuffer;

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
  private static final ByteBuffer MULTIPLIER_BUFFER = ByteBuffer.wrap(MULTIPLIER);
  private static final byte[] INCREMENT =
      {0x00000058, 0x00000051, 0xfffffff4, 0x0000002d, 0x0000004c, 0xffffff95, 0x0000007f,
          0x0000002d, 0x00000014, 0x00000005, 0x0000007b, 0x0000007e, 0xfffffff7, 0x00000067,
          0xffffff81, 0x0000004f};
  private static final ByteBuffer INCREMENT_BUFFER = ByteBuffer.wrap(INCREMENT);
  private static final int WANTED_OP_BITS = 6;
  public static final int ROTATION1 = (WANTED_OP_BITS + Long.SIZE) / 2;
  private static final int ROTATION2 = (Long.SIZE - WANTED_OP_BITS);
  private static final int ROTATION3 = (Long.SIZE * 2) - WANTED_OP_BITS;
  private static final int MASK = (1 << WANTED_OP_BITS) - 1;

  private static final ThreadLocal<ByteBuffer> rot = makeByteArrayThreadLocal();
  private static final ThreadLocal<ByteBuffer> OLD_SEED = makeByteArrayThreadLocal();
  private static final ThreadLocal<ByteBuffer> XORSHIFTED = makeByteArrayThreadLocal();
  private static final ThreadLocal<ByteBuffer> XORSHIFTED_2 = makeByteArrayThreadLocal();
  private static final ThreadLocal<ByteBuffer> RESULT_TERM_1 = makeByteArrayThreadLocal();
  private static final ThreadLocal<ByteBuffer> RESULT_TERM_2 = makeByteArrayThreadLocal();
  private static final ThreadLocal<ByteBuffer> CUR_MULT = makeByteArrayThreadLocal();
  private static final ThreadLocal<ByteBuffer> CUR_PLUS = makeByteArrayThreadLocal();
  private static final ThreadLocal<ByteBuffer> ACC_MULT = makeByteArrayThreadLocal();
  private static final ThreadLocal<ByteBuffer> ACC_PLUS = makeByteArrayThreadLocal();
  private static final ThreadLocal<ByteBuffer> ADJ_MULT = makeByteArrayThreadLocal();
  public static final double RANDOM_DOUBLE_INCR = 0x1.0p-53;

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

  @Override protected boolean usesByteBuffer() {
    return true;
  }

  @Override public synchronized void setSeed(long seed) {
    fallbackSetSeedIfInitialized();
  }

  @Override protected double nextDoubleNoEntropyDebit() {
    return (nextLongNoEntropyDebit() >> (Long.SIZE - ENTROPY_OF_DOUBLE)) * RANDOM_DOUBLE_INCR;
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
    ByteBuffer curMult = copyInto(CUR_MULT, MULTIPLIER);
    ByteBuffer curPlus = copyInto(CUR_PLUS, INCREMENT);
    ByteBuffer accMult = copyInto(ACC_MULT, Byte16ArrayArithmetic.ONE);
    ByteBuffer accPlus = copyInto(ACC_PLUS, Byte16ArrayArithmetic.ZERO);
    while (delta != 0) {
      if ((delta & 1) == 1) {
        multiplyInto(accMult, curMult);
        multiplyInto(accPlus, curMult);
        addInto(accPlus, curPlus);
      }
      ByteBuffer adjMult = copyInto(ADJ_MULT, curMult.array());
      addInto(adjMult, 1, true);
      multiplyInto(curPlus, adjMult);
      multiplyInto(curMult, curMult);
      delta >>>= 1;
    }
    lock.lock();
    try {
      multiplyInto(seedBuffer, accMult);
      addInto(seedBuffer, accPlus);
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
    return convertBytesToInt(result, Long.BYTES) >>> (Integer.SIZE - bits);
  }

  @Override protected long nextLongNoEntropyDebit() {
    byte[] result = internalNext();
    return BinaryUtils.convertBytesToLong(result, Long.BYTES);
  }

  private byte[] internalNext() {
    lock.lock();
    byte[] oldSeed;
    try {
      oldSeed = copyInto(OLD_SEED, seed).array();
      multiplyInto(seedBuffer, MULTIPLIER_BUFFER);
      addInto(seedBuffer, INCREMENT_BUFFER);
    } finally {
      lock.unlock();
    }

    // int xorshifted = (int) (((oldInternal >>> ROTATION1) ^ oldInternal) >>> ROTATION2);
    ByteBuffer xorshifted = copyInto(XORSHIFTED, oldSeed);
    ByteBuffer xorshifted2 = copyInto(XORSHIFTED_2, oldSeed);
    unsignedShiftRight(xorshifted2, ROTATION1);
    xorInto(xorshifted, xorshifted2);
    unsignedShiftRight(xorshifted, ROTATION2);

    // int rot = (int) (oldInternal >>> ROTATION3);
    ByteBuffer rot = copyInto(Pcg128Random.rot, oldSeed);
    unsignedShiftRight(rot, ROTATION3);
    final int nRot = convertBytesToInt(rot.array(), SEED_SIZE_BYTES - Integer.BYTES);

    // return ((xorshifted >>> rot) | (xorshifted << ((-rot) & MASK)))
    ByteBuffer resultTerm1 = copyInto(RESULT_TERM_1, xorshifted.array());
    unsignedShiftRight(resultTerm1, nRot);
    ByteBuffer resultTerm2 = copyInto(RESULT_TERM_2, xorshifted.array());
    Byte16ArrayArithmetic.unsignedShiftLeft(resultTerm2, (2 * Long.SIZE) - nRot);
    orInto(resultTerm2, resultTerm1);
    return resultTerm2.array();
  }

  @Override protected ToStringHelper addSubclassFields(ToStringHelper original) {
    return original;
  }

  @Override public int getNewSeedLength() {
    return SEED_SIZE_BYTES;
  }
}
