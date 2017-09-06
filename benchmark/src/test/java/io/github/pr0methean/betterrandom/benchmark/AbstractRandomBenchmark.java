/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.github.pr0methean.betterrandom.benchmark;

import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.openjdk.jmh.annotations.Benchmark;

public abstract class AbstractRandomBenchmark {

  public AbstractRandomBenchmark() {}

  protected final static RandomSeederThread seederThread = RandomSeederThread.getInstance(
      DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  protected final static byte[][] lotsOfBytes = new byte[10][10_000];
  protected final static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(10);

  protected abstract Random createPrng(@UnknownInitialization AbstractRandomBenchmark this)
      throws SeedException;

  private void innerTestBytesSequential(Random prng) {
    for (byte[] bytes : lotsOfBytes) {
      prng.nextBytes(bytes);
    }
  }

  @Benchmark
  public void testBytesSequential() throws SeedException {
    innerTestBytesSequential(createPrng());
  }

  @Benchmark
  public void testBytesSequentialReseeding() throws SeedException {
    Random prng = createPrng();
    seederThread.add(prng);
    innerTestBytesSequential(prng);
    seederThread.remove(prng);
  }

  private void innerTestBytesParallel(Random prng) throws SeedException, InterruptedException {
    for (byte[] bytes : lotsOfBytes) {
      executor.execute(() -> prng.nextBytes(bytes));
    }
    assert executor.awaitTermination(1800, TimeUnit.SECONDS) : "Timed out";
  }
  @Benchmark
  public void testBytesParallel() throws SeedException, InterruptedException {
    innerTestBytesParallel(createPrng());
  }

  @Benchmark
  public void testBytesParallelReseeding() throws SeedException, InterruptedException {
    Random prng = createPrng();
    seederThread.add(prng);
    innerTestBytesParallel(prng);
    seederThread.remove(prng);
  }
}
