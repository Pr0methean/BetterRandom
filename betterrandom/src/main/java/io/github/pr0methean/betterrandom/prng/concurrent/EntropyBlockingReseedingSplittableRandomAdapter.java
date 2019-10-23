package io.github.pr0methean.betterrandom.prng.concurrent;

import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertBytesToLong;

import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeederThread;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link ReseedingSplittableRandomAdapter} that blocks waiting to be reseeded if its entropy
 * drops too low. Unlike with {@link EntropyBlockingSplittableRandomAdapter}, reseeding is done on a
 * {@link SimpleRandomSeederThread} rather than on the calling thread.
 */
public class EntropyBlockingReseedingSplittableRandomAdapter extends ReseedingSplittableRandomAdapter {

  private static final long serialVersionUID = -779886405514766937L;
  private final AtomicReference<SeedGenerator> sameThreadSeedGen;
  private final long minimumEntropy;

  /**
   * Creates an instance.
   *
   * @param randomSeeder the {@link SimpleRandomSeederThread} that will reseed this. Its seed
   *     generator is also used on the calling thread to generate an initial seed when this
   *     {@link EntropyBlockingReseedingSplittableRandomAdapter} is used the first time on each
   *     thread.
   * @param minimumEntropy the minimum entropy; operations that would drop the entropy below this
   *     amount will instead block until the calling thread's PRNG is reseeded. Should generally
   *     be zero or negative.
   */
  public EntropyBlockingReseedingSplittableRandomAdapter(
      SimpleRandomSeederThread randomSeeder, long minimumEntropy) {
    this(randomSeeder.getSeedGenerator(), randomSeeder, minimumEntropy);
  }

  /**
   * Creates an instance.
   *
   * @param seedGenerator the seed generator that will generate an initial PRNG seed for each thread
   * @param randomSeeder the {@link SimpleRandomSeederThread} that will reseed this
   * @param minimumEntropy the minimum entropy; operations that would drop the entropy below this
   *     amount will instead block until the calling thread's PRNG is reseeded. Should generally
   *     be zero or negative.
   */
  public EntropyBlockingReseedingSplittableRandomAdapter(
      SeedGenerator seedGenerator, SimpleRandomSeederThread randomSeeder, long minimumEntropy) {
    super(seedGenerator, randomSeeder);
    this.minimumEntropy = minimumEntropy;
    this.sameThreadSeedGen = new AtomicReference<>(seedGenerator);
    if (minimumEntropy > 0) {
      throw new IllegalArgumentException("Need to be able to output 64 bits at once");
    }
  }

  @Override protected BaseRandom createDelegate() {
    EntropyBlockingRandomWrapper threadAdapter =
        new EntropyBlockingRandomWrapper(
            new SingleThreadSplittableRandomAdapter(sameThreadSeedGen.get()),
            minimumEntropy, sameThreadSeedGen.get());
    threadAdapter.setRandomSeeder(this.randomSeeder.get());
    return threadAdapter;
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
   * Returns the calling thread's seed, not the master seed.
   * @return the seed for the calling thread
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
