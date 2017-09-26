package io.github.pr0methean.betterrandom.prng.adapter;

import static org.checkerframework.checker.nullness.NullnessUtil.castNonNull;

import io.github.pr0methean.betterrandom.util.BinaryUtils;
import io.github.pr0methean.betterrandom.util.LogPreFormatter;
import java.lang.reflect.Field;
import java.util.SplittableRandom;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

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
  private static final long GOLDEN_GAMMA = 0x9e3779b97f4a7c15L;
  private static @MonotonicNonNull Field SEED_FIELD;
  private static @MonotonicNonNull Field GAMMA_FIELD;
  private static boolean CAN_RESEED_REFLECTIVELY;

  static {
    SPLITTABLE_RANDOM_CLASS = SplittableRandom.class;
    CAN_RESEED_REFLECTIVELY = false;
    try {
      GAMMA_FIELD = SPLITTABLE_RANDOM_CLASS.getDeclaredField("gamma");
      GAMMA_FIELD.setAccessible(true);
      SEED_FIELD = SPLITTABLE_RANDOM_CLASS.getDeclaredField("seed");
      SEED_FIELD.setAccessible(true);
      CAN_RESEED_REFLECTIVELY = true;
    } catch (final Exception e) {
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
        castNonNull(SEED_FIELD).setLong(original, seed);
        castNonNull(GAMMA_FIELD).setLong(original, GOLDEN_GAMMA);
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
        return BinaryUtils.convertLongToBytes(castNonNull(SEED_FIELD).getLong(splittableRandom));
      } catch (final Throwable t) {
        throw new RuntimeException(t);
      }
    } else {
      throw new UnsupportedOperationException();
    }
  }
}
