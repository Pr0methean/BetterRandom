package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.util.EntryPoint;
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

@EntryPoint
@State(Scope.Benchmark)
abstract class AbstractRandomBenchmark<T extends Random> {

  protected T prng;

  @Setup(Level.Trial) public void setUp() throws Exception {
    prng = createPrng();
  }

  @EntryPoint protected abstract T createPrng() throws Exception;

  @Timeout(time = 60) // seconds per iteration
  @Measurement(iterations = 5, time = 4)
  @Warmup(iterations = 5, time = 4)
  @EntryPoint
  @Benchmark public void testNextInt(Blackhole blackhole) {
    blackhole.consume(prng.nextInt());
  }

  @Timeout(time = 60) // seconds per iteration
  @Measurement(iterations = 5, time = 4)
  @Warmup(iterations = 5, time = 4)
  @EntryPoint
  @Benchmark public void testNextLong(Blackhole blackhole) {
    blackhole.consume(prng.nextLong());
  }
}
