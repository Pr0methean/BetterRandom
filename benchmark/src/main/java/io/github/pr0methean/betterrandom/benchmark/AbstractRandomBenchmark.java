package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

@State(Scope.Benchmark)
public abstract class AbstractRandomBenchmark {

  private static final int COLUMNS = 2;
  private static final int ROWS = 10_000;
  protected final byte[][] bytes = new byte[COLUMNS][ROWS];
  protected final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);
  protected final ExecutorCompletionService<Void> completion =
      new ExecutorCompletionService<>(executor);
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

  protected byte innerTestBytesContended() throws InterruptedException, ExecutionException {
    for (final byte[] column : bytes) {
      completion.submit(() -> prng.nextBytes(column), null);
    }
    // Wait for all jobs to finish
    while (true) {
      Future<Void> future = completion.poll();
      if (future == null) {
        break;
      } else {
        future.get();
      }
    }
    return bytes[prng.nextInt(COLUMNS)][prng.nextInt(ROWS)];
  }

  @Benchmark
  public byte testBytesContended() throws SeedException, InterruptedException, ExecutionException {
    return innerTestBytesContended();
  }

  @TearDown
  public void tearDown() {
    if (!(executor.shutdownNow().isEmpty())) {
      throw new AssertionError("Thread pool had unfinished tasks at teardown");
    }
  }
}
