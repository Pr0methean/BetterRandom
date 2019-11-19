package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.util.EntryPoint;
import java.util.concurrent.ThreadLocalRandom;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Timeout;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@EntryPoint
public class VanillaThreadLocalRandomBenchmark {

  @Timeout(time = 60) // seconds per iteration
  @Measurement(iterations = 5, time = 10)
  @Warmup(iterations = 5, time = 10)
  @Benchmark public void testNextInt(Blackhole blackhole) {
    blackhole.consume(ThreadLocalRandom.current().nextInt());
  }

  @Timeout(time = 60) // seconds per iteration
  @Measurement(iterations = 5, time = 10)
  @Warmup(iterations = 5, time = 10)
  @Benchmark public void testNextLong(Blackhole blackhole) {
    blackhole.consume(ThreadLocalRandom.current().nextLong());
  }
}
