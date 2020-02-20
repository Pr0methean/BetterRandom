package io.github.pr0methean.betterrandom.prng.adapter;

import static io.github.pr0methean.betterrandom.util.BinaryUtils.convertBytesToLong;

import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeeder;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

/**
 * A {@link SplittableRandomAdapter} that blocks waiting to be reseeded if its entropy
 * drops too low. Reseeding is done on a {@link SimpleRandomSeeder} if provided, and on the calling
 * thread otherwise. Entropy count is thread-local,
 * so consuming entropy on one thread won't directly cause blocking on another thread.
 */
public class EntropyBlockingSplittableRandomAdapter extends SplittableRandomAdapter {

  private static final long serialVersionUID = -779886405514766937L;
  private final AtomicReference<SeedGenerator> sameThreadSeedGen;
  private final long minimumEntropy;

  /**
   * Creates an instance.
   *
   * @param randomSeeder the {@link SimpleRandomSeeder} that will reseed this. Its seed
   *     generator is also used on the calling thread to generate an initial seed when this
   *     {@link EntropyBlockingSplittableRandomAdapter} is used the first time on each
   *     thread.
   * @param minimumEntropy the minimum entropy; operations that would drop the entropy below this
   *     amount will instead block until the calling thread's PRNG is reseeded. Should generally
   *     be zero or negative.
   */
  public EntropyBlockingSplittableRandomAdapter(
      SimpleRandomSeeder randomSeeder, long minimumEntropy) {
    this(randomSeeder.getSeedGenerator(), randomSeeder, minimumEntropy);
  }

  /**
   * Creates an instance.
   *
   * @param seedGenerator the seed generator that will generate an initial PRNG seed for each thread
   * @param randomSeeder the {@link SimpleRandomSeeder} that will reseed this
   * @param minimumEntropy the minimum entropy; operations that would drop the entropy below this
   *     amount will instead block until the calling thread's PRNG is reseeded. Should generally
   *     be zero or negative.
   */
  public EntropyBlockingSplittableRandomAdapter(
      @Nullable SeedGenerator seedGenerator, SimpleRandomSeeder randomSeeder, long minimumEntropy) {
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
