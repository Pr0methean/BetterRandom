package io.github.pr0methean.betterrandom.prng.concurrent;

import io.github.pr0methean.betterrandom.prng.EntropyBlockingHelper;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import java.util.concurrent.atomic.AtomicReference;

public class EntropyBlockingSplittableRandomAdapter extends SplittableRandomAdapter {
  private static final long serialVersionUID = 4992825526245524633L;
  private final EntropyBlockingHelper helper;
  private final AtomicReference<SeedGenerator> sameThreadSeedGen;

  public EntropyBlockingSplittableRandomAdapter(long minimumEntropy, SeedGenerator seedGenerator) {
    sameThreadSeedGen = new AtomicReference<>(seedGenerator);
    helper = new EntropyBlockingHelper(minimumEntropy, sameThreadSeedGen, this);
  }
}
