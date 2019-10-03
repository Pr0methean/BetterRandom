package io.github.pr0methean.betterrandom.seed;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class FakeSeedGenerator implements SeedGenerator {

  private static final long serialVersionUID = 2310664903337315190L;
  protected final String name;

  private final AtomicLong calls = new AtomicLong(0);
  private final AtomicBoolean throwException = new AtomicBoolean(false);

  public FakeSeedGenerator() {
    this("FakeSeedGenerator");
  }

  /**
   * Creates a named instance.
   *
   * @param name the name of this FakeSeedGenerator, returned by {@link #toString()}
   */
  public FakeSeedGenerator(final String name) {
    this.name = name;
  }

  @Override public void generateSeed(final byte[] output) throws SeedException {
    statusChecks();
    Arrays.fill(output, (byte) 1);
  }

  protected void statusChecks() {
    calls.incrementAndGet();
    if (throwException.get()) {
      throw new SeedException("FakeSeedGenerator configured to throw exception");
    }
  }

  @Override public String toString() {
    return name;
  }

  @Override public boolean equals(final Object o) {
    return this == o ||
        (o instanceof FakeSeedGenerator && name.equals(((FakeSeedGenerator) o).name));
  }

  @Override public int hashCode() {
    return name.hashCode();
  }

  public long countCalls() {
    return calls.get();
  }

  public void reset() {
    calls.set(0);
  }

  public void setThrowException(boolean throwException) {
    this.throwException.set(throwException);
  }
}
