package io.github.pr0methean.betterrandom.prng.adapter;

import java.lang.reflect.Field;
import java.util.SplittableRandom;

/**
 * Utility class to re-seed a {@link java.util.SplittableRandom} instance using reflection and
 * {@link sun.misc.Unsafe#putLongVolatile(Object, long, long)}. Since SplittableRandom doesn't
 * support reseeding through its public API, this is the only way a
 * ReseedingSplittableRandomAdapter can avoid a ridiculous load on the garbage collector.
 *
 * @author ubuntu
 * @version $Id: $Id
 */
public final class SplittableRandomReseeder {
  protected static final Class<?> SPLITTABLE_RANDOM_CLASS;
  private static Field SEED_FIELD;
  private static Field GAMMA_FIELD;
  private static final long GOLDEN_GAMMA = 0x9e3779b97f4a7c15L;

  static {
    SPLITTABLE_RANDOM_CLASS = SplittableRandom.class;
    try {
      SEED_FIELD = SPLITTABLE_RANDOM_CLASS.getDeclaredField("seed");
      SEED_FIELD.setAccessible(true);
      GAMMA_FIELD = SPLITTABLE_RANDOM_CLASS.getDeclaredField("gamma");
      GAMMA_FIELD.setAccessible(true);
    } catch (final Exception e) {
      throw new RuntimeException(e);
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
  public static SplittableRandom reseed(final SplittableRandom original,
      final long seed) {
    try {
      SEED_FIELD.setLong(original, seed);
      GAMMA_FIELD.setLong(original, GOLDEN_GAMMA);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    return original;
  }

  public static long getSeed(final SplittableRandom splittableRandom) {
    try {
      return SEED_FIELD.getLong(splittableRandom);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
