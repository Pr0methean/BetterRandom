package io.github.pr0methean.betterrandom.benchmark;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class Main {
  private static final double DEFAULT_MIN_OPS_PER_SEC_NEXT_INT_1_THREAD = 1.39e7;
  private static final double DEFAULT_MINIMUM_OPS_PER_SEC_LONG_1_THREAD = 8e6;
  private static final double DEFAULT_MIN_OPS_PER_SEC_NEXT_INT_2_THREADS = 2.9e6;
  private static final double DEFAULT_MINIMUM_OPS_PER_SEC_LONG_2_THREADS = 2.6e6;
  static final ImmutableList<ImmutableMap<String, Double>> MINIMUM_OPS;

  static {
    ImmutableList.Builder<ImmutableMap<String, Double>> listBuilder = ImmutableList.builder();
    ImmutableMap.Builder<String, Double> builder =
        zeroMinimumsFor(VanillaJavaRandomBenchmark.class,
            ZRandomWrapperSecureRandomBenchmark.class,
            ZVanillaJavaSecureRandomBenchmark.class);
    setMinimumNextInt(builder, AesCounterRandomBenchmark.class, 8e6);
    setMinimumNextLong(builder, AesCounterRandomBenchmark.class, 3.5e6);
    setMinimumNextLong(builder, Cmwc4096RandomBenchmark.class, 5.9e6);
    setMinimumNextLong(builder, MersenneTwisterRandomBenchmark.class, 6.8e6);
    setMinimumNextInt(builder, Pcg64RandomBenchmark.class, 3.5e7);
    setMinimumNextInt(builder, Pcg128RandomBenchmark.class, 7.8e6);
    setMinimumNextLong(builder, Pcg128RandomBenchmark.class, 7.8e6);
    setMinimumNextInt(builder, ReseedingThreadLocalRandomWrapperAesCounterRandom128Benchmark.class, 1.4e6);
    setMinimumNextLong(builder, ReseedingThreadLocalRandomWrapperAesCounterRandom128Benchmark.class, 1e6);
    setMinimumNextInt(builder, SplittableRandomAdapterBenchmark.class, 4.8e6);
    setMinimumNextLong(builder, SplittableRandomAdapterBenchmark.class, 4.4e6);
    setMinimumNextInt(builder, ThreadLocalRandomWrapperAesCounterRandom128Benchmark.class, 8e6);
    setMinimumNextLong(builder, ThreadLocalRandomWrapperAesCounterRandom128Benchmark.class, 3.6e6);
    setMinimumNextLong(builder, XorShiftRandomBenchmark.class, 7.5e6);
    listBuilder.add(builder.build());

    builder = Main
        .zeroMinimumsFor(VanillaJavaRandomBenchmark.class,
            ZRandomWrapperSecureRandomBenchmark.class,
            ZVanillaJavaSecureRandomBenchmark.class);
    setMinimumNextLong(builder, AesCounterRandomBenchmark.class, 2.3e6);
    setMinimumNextInt(builder, Cmwc4096RandomBenchmark.class, 3.4e6);
    setMinimumNextLong(builder, Cmwc4096RandomBenchmark.class, 2.2e6);
    setMinimumNextInt(builder, MersenneTwisterRandomBenchmark.class, 3.5e6);
    setMinimumNextLong(builder, MersenneTwisterRandomBenchmark.class, 2.3e6);
    setMinimumNextInt(builder, Pcg128RandomBenchmark.class, 2.2e6);
    setMinimumNextLong(builder, Pcg128RandomBenchmark.class, 2.2e6);
    setMinimumNextInt(builder, ReseedingThreadLocalRandomWrapperAesCounterRandom128Benchmark.class, 1.9e6);
    setMinimumNextLong(builder, ReseedingThreadLocalRandomWrapperAesCounterRandom128Benchmark.class, 1.1e6);
    setMinimumNextLong(builder, SplittableRandomAdapterBenchmark.class, 2.4e6);
    setMinimumNextInt(builder, ThreadLocalRandomWrapperAesCounterRandom128Benchmark.class, 6e6);
    setMinimumNextLong(builder, ThreadLocalRandomWrapperAesCounterRandom128Benchmark.class, 3e6);
    setMinimumNextInt(builder, XorShiftRandomBenchmark.class, 3.4e6);
    setMinimumNextLong(builder, XorShiftRandomBenchmark.class, 2.4e6);
    listBuilder.add(builder.build());
    MINIMUM_OPS = listBuilder.build();
  }

  static void setMinimumNextInt(ImmutableMap.Builder<String, Double> builder,
      final Class<? extends AbstractRandomBenchmark<?>> clazz, final double minimum) {
    setMinimum(builder, clazz, ".testNextInt", minimum);
  }

  static void setMinimumNextLong(ImmutableMap.Builder<String, Double> builder,
      final Class<? extends AbstractRandomBenchmark<?>> clazz, final double minimum) {
    setMinimum(builder, clazz, ".testNextLong", minimum);
  }

  private static void setMinimum(ImmutableMap.Builder<String, Double> builder,
      Class<? extends AbstractRandomBenchmark<?>> aesCounterRandomBenchmarkClass, String suffix,
      double minimum) {
    builder.put(aesCounterRandomBenchmarkClass.getName() + suffix, minimum);
  }

  @SafeVarargs static ImmutableMap.Builder<String, Double> zeroMinimumsFor(
      Class<? extends AbstractRandomBenchmark<?>>... benchmarkClasses) {
    ImmutableMap.Builder<String, Double> builder = ImmutableMap.builder();
    for (Class<? extends AbstractRandomBenchmark<?>> clazz : benchmarkClasses) {
      setMinimumNextLong(builder, clazz, 0.0);
      setMinimumNextInt(builder, clazz, 0.0);
    }
    return builder;
  }

  @SuppressWarnings("ObjectAllocationInLoop") public static void main(final String[] args)
      throws RunnerException {
    final ChainedOptionsBuilder options =
        new OptionsBuilder().syncIterations(true).shouldFailOnError(true)
            .forks(1).resultFormat(ResultFormatType.CSV).detectJvmArgs();
    boolean failed = false;
    StringBuilder failureMessage = new StringBuilder(
        String.format("The following tests are too slow:%n"));
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
            defaultMinimum = (nThreads == 2)
                ? DEFAULT_MIN_OPS_PER_SEC_NEXT_INT_2_THREADS
                : DEFAULT_MIN_OPS_PER_SEC_NEXT_INT_1_THREAD;
            break;
          case "testNextLong":
            defaultMinimum = (nThreads == 2)
                ? DEFAULT_MINIMUM_OPS_PER_SEC_LONG_2_THREADS
                : DEFAULT_MINIMUM_OPS_PER_SEC_LONG_1_THREAD;
            break;
          default:
            throw new AssertionError(
                "No minimum throughput specified for " + name);
        }
        double minimum = MINIMUM_OPS.get(nThreads - 1).getOrDefault(name, defaultMinimum);
        double score = runResult.getAggregatedResult().getPrimaryResult().getScore();
        if (score < minimum) {
          failed = true;
          failureMessage.append(String.format("%s: %f < %f%n", name, score, minimum));
        }
      }
      if (failed) {
        // Windows swallows exception message after 1st line, so dump it
        System.err.println(failureMessage);
        throw new AssertionError(failureMessage.toString());
      }
    }
  }
}
