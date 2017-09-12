package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.LogPreFormatter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.SplittableRandom;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

/**
 * Utility class to re-seed a {@link java.util.SplittableRandom} instance using reflection and
 * {@link sun.misc.Unsafe#putLongVolatile(Object, long, long)}. Since SplittableRandom doesn't
 * support reseeding through its public API, this is the only way a {@link
 * ReseedingSplittableRandomAdapter} can avoid a ridiculous load on the garbage collector.
 *
 * @author ubuntu
 * @version $Id: $Id
 */
@SuppressWarnings({"argument.type.incompatible", "CanBeFinal"})
// Field.get(null) is OK when the field is static!
public final class SplittableRandomReseeder {

  private static final LogPreFormatter LOG = new LogPreFormatter(SplittableRandomReseeder.class);
  private static final Objenesis OBJENESIS = new ObjenesisStd();
  private static @Nullable MethodHandle PUT_LONG_VOLATILE;
  private static @Nullable Field SEED_FIELD;
  private static long GAMMA_FIELD_OFFSET;
  private static long SEED_FIELD_OFFSET;
  private static long GOLDEN_GAMMA;
  private static boolean CAN_RESEED_REFLECTIVELY;

  static {
    try {
      final Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
      Object unsafe;
      try {
        final Field unsafeInstance = unsafeClass.getDeclaredField("theUnsafe");
        unsafeInstance.setAccessible(true);
        unsafe = unsafeInstance.get(null);
      } catch (NoSuchFieldException | IllegalAccessException e) {
        unsafe = OBJENESIS.newInstance(unsafeClass);
      }
      final Method getFieldOffset = unsafeClass.getDeclaredMethod("objectFieldOffset", Field.class);
      GAMMA_FIELD_OFFSET = (long) (getFieldOffset
          .invoke(unsafe, SplittableRandom.class.getDeclaredField("gamma")));
      SEED_FIELD = SplittableRandom.class.getDeclaredField("seed");
      SEED_FIELD.setAccessible(true);
      SEED_FIELD_OFFSET = (long) (getFieldOffset
          .invoke(unsafe, SEED_FIELD));
      final Field goldenGammaField = SplittableRandom.class.getDeclaredField("GOLDEN_GAMMA");
      goldenGammaField.setAccessible(true);
      GOLDEN_GAMMA = (long) (goldenGammaField.get(null));
      final Method putVolatileLong = unsafeClass
          .getDeclaredMethod("putLongVolatile", Object.class, long.class, long.class);
      putVolatileLong.setAccessible(true);
      PUT_LONG_VOLATILE = MethodHandles.lookup().unreflect(putVolatileLong).bindTo(unsafe);
      CAN_RESEED_REFLECTIVELY = true;
    } catch (final Exception e) {
      // May include at least one exception type that's new in Java 9
      LOG.error("Can't reflectively reseed SplittableRandom instances: %s", e);
      CAN_RESEED_REFLECTIVELY = false;
    }
  }

  private SplittableRandomReseeder() {
  }

  /**
   * Reseeds if possible, or else replaces, a {@link SplittableRandom}.
   *
   * @param original a {@link SplittableRandom}.
   * @param seed a seed.
   * @return a {@link SplittableRandom} whose seed is {@code seed}, and if possible is the same
   *     object as {@code original}.
   */
  public static SplittableRandom reseed(final @Nullable SplittableRandom original,
      final long seed) {
    if (CAN_RESEED_REFLECTIVELY && PUT_LONG_VOLATILE != null && original != null) {
      try {
        PUT_LONG_VOLATILE.invokeExact((Object) original, SEED_FIELD_OFFSET, seed);
        PUT_LONG_VOLATILE.invokeExact((Object) original, GAMMA_FIELD_OFFSET, GOLDEN_GAMMA);
        return original;
      } catch (final Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    } else {
      return new SplittableRandom(seed);
    }
  }

  public static byte[] getSeed(SplittableRandom splittableRandom) {
    if (SEED_FIELD == null) {
      throw new UnsupportedOperationException();
    } else {
      try {
        return BinaryUtils.convertLongToBytes((long) (SEED_FIELD.get(splittableRandom)));
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
