package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public abstract class AbstractRandomBenchmark {

  private static final int COLUMNS = 2;
  private static final int ROWS = 50_000;
  protected final byte[][] bytes = new byte[COLUMNS][ROWS];
  protected final Random prng;

  {
    try {
      prng = createPrng();
    } catch (final SeedException e) {
      throw new AssertionError(e);
    }
  }

  protected abstract Random createPrng(@UnknownInitialization AbstractRandomBenchmark this)
      throws SeedException;

  protected byte innerTestBytesSequential() {
    for (final byte[] column : bytes) {
      prng.nextBytes(column);
    }
    return bytes[prng.nextInt(COLUMNS)][prng.nextInt(ROWS)];
  }

  @Benchmark
  public byte testBytesSequential() {
    return innerTestBytesSequential();
  }

  protected byte innerTestBytesContended() throws InterruptedException {
    Thread[] threads = new Thread[COLUMNS];
    for (int column = 0; column < COLUMNS; column++) {
      threads[column] = new Thread(() -> prng.nextBytes(bytes[column]));
      threads[column].start();
    }
    // Wait for the threads to finish
    for (Thread thread : threads) {
      thread.join();
    }
    return bytes[prng.nextInt(COLUMNS)][prng.nextInt(ROWS)];
  }

  @Benchmark
  public byte testBytesContended() throws SeedException, InterruptedException {
    return innerTestBytesContended();
  }
}
