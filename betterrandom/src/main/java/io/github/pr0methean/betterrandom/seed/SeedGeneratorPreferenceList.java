package io.github.pr0methean.betterrandom.seed;

import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A {@link SeedGenerator} implementation that iterates over multiple delegates until one succeeds.
 */
public class SeedGeneratorPreferenceList extends CopyOnWriteArrayList<SeedGenerator>
    implements SeedGenerator {

  private static final long serialVersionUID = -4429919137592899776L;
  private final boolean isAlwaysWorthTrying;

  /**
   * Creates an instance.
   *
   * @param contents the initial list of delegates, in the order they will be tried until one
   *     succeeds
   * @param isAlwaysWorthTrying true if {@link #isWorthTrying()} should always return true rather
   *     than delegating
   */
  @EntryPoint public SeedGeneratorPreferenceList(Collection<? extends SeedGenerator> contents,
      boolean isAlwaysWorthTrying) {
    addAll(contents);
    this.isAlwaysWorthTrying = isAlwaysWorthTrying;
  }

  /**
   * Creates an instance.
   *
   * @param contents the initial delegates, in the order they will be tried until one succeeds
   * @param isAlwaysWorthTrying true if {@link #isWorthTrying()} should always return true rather
   *     than delegating
   */
  @EntryPoint public SeedGeneratorPreferenceList(boolean isAlwaysWorthTrying, SeedGenerator...
      contents) {
    this(Arrays.asList(contents), isAlwaysWorthTrying);
  }

  @Override public void generateSeed(byte[] output) throws SeedException {
    for (final SeedGenerator generator : this) {
      if (generator.isWorthTrying()) {
        try {
          generator.generateSeed(output);
          return;
        } catch (final SeedException ignored) {
          // Try the next one
        }
      }
    }
    throw new SeedException("All available seed generation strategies failed.");
  }

  @Override public byte[] generateSeed(final int length) throws SeedException {
    if (length <= 0) {
      return EMPTY_SEED;
    }
    final byte[] output = new byte[length];
    generateSeed(output);
    return output;
  }

  @Override public boolean isWorthTrying() {
    if (isAlwaysWorthTrying) {
      return true;
    }
    for (final SeedGenerator generator : this) {
      if (generator.isWorthTrying()) {
        return true;
      }
    }
    return false;
  }
}
