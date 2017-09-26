package io.github.pr0methean.betterrandom.prng.adapter;

import static org.checkerframework.checker.nullness.NullnessUtil.castNonNull;

import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.LogPreFormatter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.SplittableRandom;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
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

  /** Visible only for test debugging. */
  protected static final Class<?> SPLITTABLE_RANDOM_CLASS;
  private static final LogPreFormatter LOG = new LogPreFormatter(SplittableRandomReseeder.class);
  private static final Objenesis OBJENESIS = new ObjenesisStd();
  private static @MonotonicNonNull MethodHandle GET_LONG_VOLATILE;
  private static @MonotonicNonNull MethodHandle PUT_LONG_VOLATILE;
  private static @MonotonicNonNull Field SEED_FIELD;
  private static @MonotonicNonNull Field GAMMA_FIELD;
  private static long GAMMA_FIELD_OFFSET;
  private static long SEED_FIELD_OFFSET;
  private static final long GOLDEN_GAMMA = 0x9e3779b97f4a7c15L;
  private static boolean CAN_RESEED_REFLECTIVELY;

  static {
    SPLITTABLE_RANDOM_CLASS = SplittableRandom.class;
    try {
      CAN_RESEED_REFLECTIVELY = false;
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
      GAMMA_FIELD = SPLITTABLE_RANDOM_CLASS.getDeclaredField("gamma");
      GAMMA_FIELD.setAccessible(true);
      SEED_FIELD = SPLITTABLE_RANDOM_CLASS.getDeclaredField("seed");
      SEED_FIELD.setAccessible(true);
      CAN_RESEED_REFLECTIVELY = true;
      GAMMA_FIELD_OFFSET = (long) (getFieldOffset
          .invoke(unsafe, GAMMA_FIELD));
      SEED_FIELD_OFFSET = (long) (getFieldOffset
          .invoke(unsafe, SEED_FIELD));
      final MethodHandles.Lookup lookup = MethodHandles.lookup();
      final Method putLongVolatile = unsafeClass
          .getDeclaredMethod("putLongVolatile", Object.class, long.class, long.class);
      putLongVolatile.setAccessible(true);
      PUT_LONG_VOLATILE = lookup.unreflect(putLongVolatile).bindTo(unsafe);
      final Method getLongVolatile = unsafeClass.getDeclaredMethod("getLongVolatile",
          Object.class, long.class);
      getLongVolatile.setAccessible(true);
      GET_LONG_VOLATILE = lookup.unreflect(getLongVolatile).bindTo(unsafe);
    } catch (final Exception e) {
      // May include at least one exception type that's new in Java 9
      LOG.error("Can't reflectively reseed SplittableRandom instances: %s", e);
    }
  }

  /** Utility class that shouldn't be instantiated. */
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
    if (CAN_RESEED_REFLECTIVELY && original != null) {
      try {
        if (PUT_LONG_VOLATILE == null) {
          castNonNull(SEED_FIELD).setLong(original, seed);
          castNonNull(GAMMA_FIELD).setLong(original, GOLDEN_GAMMA);
        } else {
          PUT_LONG_VOLATILE.invokeExact((Object) original, SEED_FIELD_OFFSET, seed);
          PUT_LONG_VOLATILE.invokeExact((Object) original, GAMMA_FIELD_OFFSET, GOLDEN_GAMMA);
        }
        return original;
      } catch (final Throwable t) {
        throw new RuntimeException(t);
      }
    } else {
      return new SplittableRandom(seed);
    }
  }

  /** @return false if {@link #getSeed(SplittableRandom)} will fail; true otherwise. */
  @EnsuresNonNullIf(result = true, expression = "SEED_FIELD")
  public static boolean canGetSeed() {
    return SEED_FIELD != null;
  }

  public static byte[] getSeed(final SplittableRandom splittableRandom) {
    if (canGetSeed()) {
      try {
        return BinaryUtils.convertLongToBytes(
            GET_LONG_VOLATILE == null
                ? castNonNull(SEED_FIELD).getLong(splittableRandom)
                : (long) (GET_LONG_VOLATILE
                    .invokeExact(splittableRandom, (long) SEED_FIELD_OFFSET)));
      } catch (final Throwable t) {
        throw new RuntimeException(t);
      }
    } else {
      throw new UnsupportedOperationException();
    }
  }
}
