package io.github.pr0methean.betterrandom.benchmark;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Timeout;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@EntryPoint
@State(Scope.Benchmark)
abstract class AbstractRandomBenchmark<T extends Random> {

  /**
   * TODO: Find a way to specify this separately for each test
   */
  private static final double DEFAULT_MIN_OPS_PER_SEC_NEXT_INT = 15_000_000;
  private static final double DEFAULT_MINIMUM_OPS_PER_SEC_LONG = 15_000_000;
  private static final List<String> NO_MIN_SCORE = ImmutableList.of(
      "VanillaJavaRandomBenchmark",
      "ZRandomWrapperSecureRandomBenchmark",
      "ZVanillaJavaSecureRandomBenchmark");
  private static final ImmutableMap<String, Double> MINIMUM_OPS
      = zeroMinimumsFor(
          VanillaJavaRandomBenchmark.class,
          ZRandomWrapperSecureRandomBenchmark.class,
          ZVanillaJavaSecureRandomBenchmark.class)
      .put(AesCounterRandomBenchmark.class.getName() + ".testNextInt", 9_000_000.0)
      .put(AesCounterRandomBenchmark.class.getName() + ".testNextLong", 9_000_000.0)
      .put(ReseedingThreadLocalRandomWrapperAesCounterRandom128Benchmark.class.getName() + ".testNextInt",
          1_800_000.0)
      .put(ReseedingThreadLocalRandomWrapperAesCounterRandom128Benchmark.class.getName() + ".testNextLong",
          1_500_000.0)
      .put(SplittableRandomAdapterBenchmark.class.getName() + ".testNextInt",
          6_000_000.0)
      .put(SplittableRandomAdapterBenchmark.class.getName() + ".testNextLong",
          6_000_000.0)
      .put(ThreadLocalRandomWrapperAesCounterRandom128Benchmark.class.getName() + ".testNextLong",
          9_000_000.0)
      .put(XorShiftRandomBenchmark.class.getName() + ".testNextLong", 9_000_000.0)
      .build();
  protected T prng;

  private static ImmutableMap.Builder<String, Double> zeroMinimumsFor(
      Class<? extends AbstractRandomBenchmark<?>>... benchmarkClasses) {
    ImmutableMap.Builder<String, Double> builder = ImmutableMap.builder();
    for (Class<? extends AbstractRandomBenchmark<?>> clazz : benchmarkClasses) {
      builder.put(clazz.getName() + ".testNextInt", 0.0)
          .put(clazz.getName() + ".testNextLong", 0.0);
    }
    return builder;
  }

  AbstractRandomBenchmark() {
  }

  @SuppressWarnings("ObjectAllocationInLoop") public static void main(final String[] args)
      throws RunnerException {
    final ChainedOptionsBuilder options =
        new OptionsBuilder().syncIterations(true).shouldFailOnError(true)
            .forks(1).resultFormat(ResultFormatType.CSV).detectJvmArgs();
    for (int nThreads = 1; nThreads <= 2; nThreads++) {
      final Runner runner = new Runner(
          options.threads(nThreads).result(String.format("%d-thread_bench_results.csv", nThreads))
              .build());
      Collection<RunResult> results = runner.run();
      if (results.isEmpty()) {
        throw new AssertionError("Empty result set");
      }
      runner.list(); // Produce default
      for (RunResult runResult : results) {
        final String name = runResult.getParams().getBenchmark();
        double defaultMinimum;
        switch (runResult.getPrimaryResult().getLabel()) {
          case "testNextInt":
            defaultMinimum = DEFAULT_MIN_OPS_PER_SEC_NEXT_INT;
            break;
          case "testNextLong":
            defaultMinimum = DEFAULT_MINIMUM_OPS_PER_SEC_LONG;
            break;
          default:
            throw new AssertionError(
                "No minimum throughput specified for " + name);
        }
        double minimum = MINIMUM_OPS.getOrDefault(name, defaultMinimum);
        double score = runResult.getAggregatedResult().getPrimaryResult().getScore();
        if (score < minimum) {
          throw new AssertionError(String.format("Score %f for %s is too slow", score, name));
        }
      }
    }
  }

  @Setup(Level.Trial) public void setUp() throws Exception {
    prng = createPrng();
  }

  @EntryPoint protected abstract T createPrng() throws Exception;

  @Timeout(time = 60) // seconds per iteration
  @Measurement(iterations = 5, time = 4)
  @Warmup(iterations = 5, time = 4)
  @Benchmark public void testNextInt(Blackhole blackhole) {
    blackhole.consume(prng.nextInt());
  }

  @Timeout(time = 60) // seconds per iteration
  @Measurement(iterations = 5, time = 4)
  @Warmup(iterations = 5, time = 4)
  @Benchmark public void testNextLong(Blackhole blackhole) {
    blackhole.consume(prng.nextLong());
  }
}
