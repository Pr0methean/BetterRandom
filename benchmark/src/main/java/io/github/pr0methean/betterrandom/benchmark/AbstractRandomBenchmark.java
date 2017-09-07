package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.profile.HotspotMemoryProfiler;
import org.openjdk.jmh.profile.HotspotRuntimeProfiler;
import org.openjdk.jmh.profile.HotspotThreadProfiler;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;

// FIXME: Get the multithreaded benchmarks working
@State(Scope.Benchmark)
public abstract class AbstractRandomBenchmark {

  private static final int COLUMNS = 2;
  private static final int ROWS = 50_000;
  protected final byte[][] bytes = new byte[COLUMNS][ROWS];
  // protected final Thread[] threads = new Thread[COLUMNS];
  protected final Random prng;

  {
    try {
      prng = createPrng();
    } catch (final SeedException e) {
      throw new AssertionError(e);
    }
  }

  /*
  @Group("contended")
  public void setUpThreads() {
    for (int column = 0; column < COLUMNS; column++) {
      final int column_ = column;
      threads[column] = new Thread(() -> prng.nextBytes(bytes[column_]));
    }
  }
  */

  public static void main(String[] args) throws RunnerException {
    ChainedOptionsBuilder options = new OptionsBuilder()
        .addProfiler(HotspotThreadProfiler.class)
        .addProfiler(HotspotRuntimeProfiler.class)
        .addProfiler(HotspotMemoryProfiler.class)
        .addProfiler(GCProfiler.class)
        .addProfiler(StackProfiler.class)
        .shouldFailOnError(true)
        .forks(1);
    for (int nThreads = 1; nThreads < 2; nThreads++) {
      new Runner(options
          .threads(nThreads)
          .output(String.format("benchmark/target/%d-thread_bench_results.txt", nThreads))
          .build()).run();
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

  /*
  protected byte innerTestBytesContended() throws InterruptedException {
    // Start the threads
    for (Thread thread : threads) {
      thread.start();
    }
    // Wait for the threads to finish
    for (Thread thread : threads) {
      thread.join();
    }
    return bytes[prng.nextInt(COLUMNS)][prng.nextInt(ROWS)];
  }
  */

  /*
  @Benchmark
  @Group("contended")
  public byte testBytesContended() throws SeedException, InterruptedException {
    return innerTestBytesContended();
  }
  */

  @TearDown(Level.Trial)
  public void gc() {
    System.gc();
  }
}
