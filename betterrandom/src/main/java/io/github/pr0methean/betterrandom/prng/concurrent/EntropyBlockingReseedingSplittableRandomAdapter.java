package io.github.pr0methean.betterrandom.prng.concurrent;

import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertBytesToLong;

import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeederThread;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicReference;

public class EntropyBlockingReseedingSplittableRandomAdapter extends ReseedingSplittableRandomAdapter {

  private final AtomicReference<SeedGenerator> sameThreadSeedGen;
  private final long minimumEntropy;

  public EntropyBlockingReseedingSplittableRandomAdapter(
      SimpleRandomSeederThread randomSeeder, long minimumEntropy) {
    this(randomSeeder.getSeedGenerator(), randomSeeder, minimumEntropy);
  }

  public EntropyBlockingReseedingSplittableRandomAdapter(
      SeedGenerator seedGenerator, SimpleRandomSeederThread randomSeeder, long minimumEntropy) {
    super(seedGenerator, randomSeeder);
    this.minimumEntropy = minimumEntropy;
    this.sameThreadSeedGen = new AtomicReference<>(seedGenerator);
    initSubclassTransientFields();
    if (minimumEntropy > 0) {
      throw new IllegalArgumentException("Need to be able to output 64 bits at once");
    }
  }

  private void initSubclassTransientFields() {
    threadLocal = ThreadLocal.withInitial(() -> {
      EntropyBlockingRandomWrapper threadAdapter =
          new EntropyBlockingRandomWrapper(
              new SingleThreadSplittableRandomAdapter(sameThreadSeedGen.get()),
              minimumEntropy, sameThreadSeedGen.get());
      threadAdapter.setRandomSeeder(this.randomSeeder.get());
      return threadAdapter;
    });
  }

  /**
   * {@inheritDoc} Applies only to the calling thread.
   */
  @Override public void setSeed(final byte[] seed) {
    checkLength(seed, Long.BYTES);
    setSeed(convertBytesToLong(seed));
  }

  /**
   * {@inheritDoc} Applies only to the calling thread.
   */
  @Override public void setSeed(
      final long seed) {
    if (this.seed == null) {
      super.setSeed(seed);
    }
    if (threadLocal == null) {
      return;
    }
    getDelegateWrapper().setSeed(seed);
  }

  @Override protected SplittableRandom getSplittableRandom() {
    return ((SingleThreadSplittableRandomAdapter)
        getDelegateWrapper().getWrapped()).getSplittableRandom();
  }

  /**
   * {@inheritDoc} Applies only to the calling thread.
   */
  @Override public byte[] getSeed() {
    return getDelegateWrapper().getSeed();
  }

  @Override protected void debitEntropy(long bits) {
    getDelegateWrapper().debitEntropy(bits);
  }

  private EntropyBlockingRandomWrapper getDelegateWrapper() {
    return (EntropyBlockingRandomWrapper) threadLocal.get();
  }
}
