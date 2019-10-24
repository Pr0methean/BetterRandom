package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.util.Random;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Timeout;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@EntryPoint
@State(Scope.Benchmark)
abstract class AbstractRandomBenchmark {

  protected static final int COLUMNS = 2;
  protected static final int ROWS = 50_000;
  @SuppressWarnings("MismatchedReadAndWriteOfArray") private final byte[][] bytes =
      new byte[COLUMNS][ROWS];
  protected Random prng;

  protected AbstractRandomBenchmark() {
  }

  @SuppressWarnings("ObjectAllocationInLoop") public static void main(final String[] args)
      throws RunnerException {
    final ChainedOptionsBuilder options =
        new OptionsBuilder().syncIterations(false).shouldFailOnError(true)
            .forks(1).resultFormat(ResultFormatType.CSV).detectJvmArgs();
    for (int nThreads = 1; nThreads <= 2; nThreads++) {
      new Runner(
          options.threads(nThreads).output(String.format("%d-thread_bench_results.csv", nThreads))
              .build()).run();
    }
  }

  @Setup(Level.Trial) public void setUp() throws Exception {
    prng = createPrng();
  }

  @EntryPoint protected abstract Random createPrng() throws Exception;

  protected byte innerTestBytesSequential() {
    for (final byte[] column : bytes) {
      prng.nextBytes(column);
    }
    return bytes[prng.nextInt(COLUMNS)][prng.nextInt(ROWS)];
  }
  @Timeout(time = 60) // seconds per iteration
  @Measurement(iterations = 5, time = 4)
  @Warmup(iterations = 5, time = 4)
  @OperationsPerInvocation(COLUMNS * ROWS)
  @Benchmark public byte testBytesSequential() {
    return innerTestBytesSequential();
  }

  @TearDown(Level.Trial)
  public void tearDown() {
    prng = null;
  }
}
