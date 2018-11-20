package io.github.pr0methean.betterrandom.seed;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public class FakeSeedGenerator implements SeedGenerator {

  private static final long serialVersionUID = 2310664903337315190L;
  private final String name;

  private final AtomicLong calls = new AtomicLong(0);

  public FakeSeedGenerator() {
    this("FakeSeedGenerator");
  }

  /**
   * Creates a named instance.
   * @param name the name of this FakeSeedGenerator, returned by {@link #toString()}
   */
  public FakeSeedGenerator(final String name) {
    this.name = name;
  }

  @Override public void generateSeed(final byte[] output) throws SeedException {
    calls.incrementAndGet();
    Arrays.fill(output, (byte) 1);
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean equals(final Object o) {
    return this == o
        || (o instanceof FakeSeedGenerator
            && name.equals(((FakeSeedGenerator) o).name));
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  public long countCalls() {
    return calls.get();
  }

  public void resetCalls() {
    calls.set(0);
  }
}
