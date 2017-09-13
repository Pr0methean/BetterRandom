package io.github.pr0methean.betterrandom.benchmark;

import com.google.common.collect.HashMultiset;
import com.google.monitoring.runtime.instrumentation.AllocationRecorder;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import io.github.pr0methean.betterrandom.util.LogPreFormatter;
import java.util.Arrays;
import java.util.Random;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.profile.HotspotMemoryProfiler;
import org.openjdk.jmh.profile.HotspotRuntimeProfiler;
import org.openjdk.jmh.profile.HotspotThreadProfiler;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;

// FIXME: Get the multithreaded benchmarks working
@State(Scope.Benchmark)
abstract class AbstractRandomBenchmark {

  private static final LogPreFormatter LOG = new LogPreFormatter(AbstractRandomBenchmark.class);

  private static final int COLUMNS = 2;
  private static final int ROWS = 50_000;
  @SuppressWarnings("MismatchedReadAndWriteOfArray")
  private final byte[][] bytes = new byte[COLUMNS][ROWS];
  private final HashMultiset<StackTrace> stackTraces = HashMultiset.create();
  // protected final Thread[] threads = new Thread[COLUMNS];
  protected Random prng;

  @EntryPoint
  public AbstractRandomBenchmark() {
  }

  @SuppressWarnings("ObjectAllocationInLoop")
  public static void main(final String[] args) throws RunnerException {
    final ChainedOptionsBuilder options = new OptionsBuilder()
        .addProfiler(HotspotThreadProfiler.class)
        .addProfiler(HotspotRuntimeProfiler.class)
        .addProfiler(HotspotMemoryProfiler.class)
        .addProfiler(GCProfiler.class)
        .addProfiler(StackProfiler.class)
        .shouldFailOnError(true)
        .forks(1)
        .resultFormat(ResultFormatType.CSV)
        .detectJvmArgs();
    for (int nThreads = 1; nThreads <= 2; nThreads++) {
      new Runner(options
          .threads(nThreads)
          .output(String.format("%d-thread_bench_results.csv", nThreads))
          .build()).run();
    }
  }

  @Setup(Level.Trial)
  public void setUp() {
    try {
      prng = createPrng();
    } catch (final SeedException e) {
      throw new AssertionError(e);
    }

    // FIXME: Why isn't this outputting anything?
    AllocationRecorder.addSampler((arrayLength, desc, newObj, size) -> {
      if (!desc.contains("StackTrace")) {
        LOG.info("Created %s (a %s of %d bytes)", newObj, desc, size);
        stackTraces.add(new StackTrace(Thread.currentThread().getStackTrace()));
      }
    });
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

  protected abstract Random createPrng() throws SeedException;

  byte innerTestBytesSequential() {
    for (final byte[] column : bytes) {
      prng.nextBytes(column);
    }
    return bytes[prng.nextInt(COLUMNS)][prng.nextInt(ROWS)];
  }

  @Benchmark
  public byte testBytesSequential() {
    return innerTestBytesSequential();
  }

  // FIXME: Why isn't this outputting anything either?
  @TearDown(Level.Trial)
  public void tearDown() {
    System.gc();
    for (final StackTrace stackTrace : stackTraces) {
      LOG.info("%d objects created from:%n%s%n", stackTraces.count(stackTrace), stackTrace);
    }
    stackTraces.clear();
  }

  /*
  protected byte innerTestBytesContended() throws InterruptedException {
    // Start the threads
    for (Thread seederThread : threads) {
      seederThread.start();
    }
    // Wait for the threads to finish
    for (Thread seederThread : threads) {
      seederThread.join();
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

  private static final class StackTrace {

    private static final int OBJECT_CREATION_STACK_DEPTH = 5;
    private static final String NEWLINE = String.format("%n");
    /** A rough estimate of the average of {@link StackTraceElement#toString()}.length(). */
    private static final int ESTIMATED_AVERAGE_STACK_TRACE_ELEMENT_SIZE = 50;
    private final StackTraceElement[] elements;

    private StackTrace(final StackTraceElement[] elements) {
      this.elements = Arrays.copyOf(elements, OBJECT_CREATION_STACK_DEPTH);
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof StackTrace)) {
        return false;
      }

      final StackTrace that = (StackTrace) o;
      return Arrays.equals(elements, that.elements);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(elements);
    }

    @Override
    public String toString() {
      final StringBuilder stringBuilder = new StringBuilder(
          elements.length * ESTIMATED_AVERAGE_STACK_TRACE_ELEMENT_SIZE);
      for (final StackTraceElement element : elements) {
        stringBuilder.append(element);
        stringBuilder.append(NEWLINE);
      }
      return stringBuilder.toString();
    }
  }
}
