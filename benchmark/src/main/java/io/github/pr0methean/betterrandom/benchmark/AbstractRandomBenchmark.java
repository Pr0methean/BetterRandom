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
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Group)
public abstract class AbstractRandomBenchmark {

  protected static final RandomSeederThread seederThread = RandomSeederThread.getInstance(
      DefaultSeedGenerator.DEFAULT_SEED_GENERATOR);
  private static final int COLUMNS = 2;
  private static final int ROWS = 10_000;
  protected final byte[][] bytes = new byte[COLUMNS][ROWS];
  protected final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);
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

  private byte innerTestBytesSequential() {
    for (final byte[] column : bytes) {
      prng.nextBytes(column);
    }
    return bytes[prng.nextInt(COLUMNS)][prng.nextInt(ROWS)];
  }

  @Benchmark
  public byte testBytesSequential() {
    return innerTestBytesSequential();
  }

  @Benchmark
  public byte testBytesSequentialReseeding() {
    seederThread.add(prng);
    final byte b = innerTestBytesSequential();
    seederThread.remove(prng);
    return b;
  }

  private byte innerTestBytesContended() throws InterruptedException {
    for (final byte[] column : bytes) {
      executor.execute(() -> prng.nextBytes(column));
    }
    assert executor.awaitTermination(1800, TimeUnit.SECONDS) : "Timed out";
    return bytes[prng.nextInt(COLUMNS)][prng.nextInt(ROWS)];
  }

  @Benchmark
  public byte testBytesContended() throws SeedException, InterruptedException {
    return innerTestBytesContended();
  }

  @Benchmark
  public byte testBytesContendedReseeding() throws SeedException, InterruptedException {
    seederThread.add(prng);
    final byte b = innerTestBytesContended();
    seederThread.remove(prng);
    return b;
  }
}
