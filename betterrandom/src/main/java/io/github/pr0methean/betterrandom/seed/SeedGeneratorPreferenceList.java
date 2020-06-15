package io.github.pr0methean.betterrandom.seed;

import com.google.common.collect.ImmutableList;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * A {@link SeedGenerator} implementation that iterates over multiple delegates until one succeeds.
 */
public class SeedGeneratorPreferenceList implements SeedGenerator {

  private final ImmutableList<SeedGenerator> delegates;
  private static final long serialVersionUID = -4429919137592899776L;
  private final boolean isAlwaysWorthTrying;

  /**
   * Creates an instance.
   *
   * @param delegates the list of delegates, in the order they will be tried until one succeeds
   * @param isAlwaysWorthTrying true if {@link #isWorthTrying()} should always return true rather
   *     than delegating
   */
  @EntryPoint public SeedGeneratorPreferenceList(Collection<? extends SeedGenerator> delegates,
      boolean isAlwaysWorthTrying) {
    this.delegates = ImmutableList.copyOf(delegates);
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
    for (final SeedGenerator generator : delegates) {
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

  @Override public boolean isWorthTrying() {
    if (isAlwaysWorthTrying) {
      return true;
    }
    for (final SeedGenerator generator : delegates) {
      if (generator.isWorthTrying()) {
        return true;
      }
    }
    return false;
  }

  @Override public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SeedGeneratorPreferenceList that = (SeedGeneratorPreferenceList) o;
    return isAlwaysWorthTrying == that.isAlwaysWorthTrying && delegates.equals(that.delegates);
  }

  @Override public int hashCode() {
    return Objects.hash(delegates, isAlwaysWorthTrying);
  }

  public static class Builder {
    private final ImmutableList.Builder<SeedGenerator> delegates = new ImmutableList.Builder<>();
    private boolean isAlwaysWorthTrying;
    public SeedGeneratorPreferenceList build() {
      return new SeedGeneratorPreferenceList(delegates.build(), isAlwaysWorthTrying);
    }
    public void add(SeedGenerator... seedGenerators) {
      delegates.add(seedGenerators);
    }
    public void addAll(Iterable<? extends SeedGenerator> seedGenerators) {
      delegates.addAll(seedGenerators);
    }
    public void setAlwaysWorthTrying(boolean alwaysWorthTrying) {
      isAlwaysWorthTrying = alwaysWorthTrying;
    }
  }
}
