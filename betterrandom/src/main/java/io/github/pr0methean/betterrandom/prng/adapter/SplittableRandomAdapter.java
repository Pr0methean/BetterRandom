package io.github.pr0methean.betterrandom.prng.adapter;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.prng.BaseRandom;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeeder;
import java.util.Objects;
import java.util.SplittableRandom;
import javax.annotation.Nullable;

/**
 * Thread-safe PRNG that wraps a {@link ThreadLocal}&lt;{@link SplittableRandom}&gt;. Registers each
 * thread's instance with a {@link SimpleRandomSeeder} to replace its {@link SplittableRandom} with
 * a reseeded one as frequently as possible, but not more frequently than it is being used.
 * <p>
 * In OpenJDK 8 and Android API 24 and later, {@link java.util.concurrent.ThreadLocalRandom} uses
 * the same PRNG algorithm as {@link SplittableRandom}, and is faster because of internal coupling
 * with {@link Thread}. As well, the instance returned by
 * {@link java.util.concurrent.ThreadLocalRandom#current()} can be safely passed to any thread that
 * has ever called {@code current()}, and streams created by a ThreadLocalRandom are safely
 * parallel. Thus, this class should only be used when reseeding or the ability to specify a seed is
 * required, or for compatibility with JDK 7 or an older Android version.
 * @author Chris Hennick
 */
public class SplittableRandomAdapter extends BaseSplittableRandomAdapter {

  private static final long serialVersionUID = 6301096404034224037L;
  private final SeedGenerator seedGenerator;
  /**
   * A thread-local delegate.
   */
  @SuppressWarnings({"ThreadLocalNotStaticFinal", "TransientFieldNotInitialized"})
  protected transient ThreadLocal<BaseRandom> threadLocal;

  /**
   * Creates an instance.
   *
   * @param seedGenerator the seed generator that will generate an initial seed for each thread
   * @param randomSeeder the {@link SimpleRandomSeeder} that will generate a seed for a new
   *     {@link SplittableRandom} instance whenever each thread's instance needs reseeding
   * @throws SeedException if {@code seedGenerator} fails to generate an initial seed
   */
  public SplittableRandomAdapter(final SeedGenerator seedGenerator,
      @Nullable SimpleRandomSeeder randomSeeder) throws SeedException {
    super(new byte[Long.BYTES]);
    this.seedGenerator = seedGenerator;
    this.randomSeeder.set(randomSeeder);
    threadLocal = ThreadLocal.withInitial(this::createDelegate);
  }

  /**
   * Creates an instance that uses the same {@link SeedGenerator} for reseeding and for initial
   * seeding, and whose {@link SimpleRandomSeeder} uses a
   * {@link SimpleRandomSeeder.DefaultThreadFactory}.
   *
   * @param seedGenerator the seed generator that will generate an initial seed for each thread
   * @throws SeedException if {@code seedGenerator} fails to generate an initial seed
   */
  public SplittableRandomAdapter(final SeedGenerator seedGenerator) {
    this(seedGenerator, new SimpleRandomSeeder(seedGenerator));
  }

  /**
   * Creates the delegate for the calling thread.
   *
   * @return the thread-local delegate
   */
  protected BaseRandom createDelegate() {
      SingleThreadSplittableRandomAdapter threadAdapter =
          new SingleThreadSplittableRandomAdapter(this.seedGenerator);
      threadAdapter.setRandomSeeder(this.randomSeeder.get());
      return threadAdapter;
  }

  @Override public long getEntropyBits() {
    return threadLocal.get().getEntropyBits();
  }

  @Override public byte[] getSeed() {
    return threadLocal.get().getSeed();
  }

  @Override public void setRandomSeeder(@Nullable final SimpleRandomSeeder randomSeeder) {
    if (!Objects.equals(this.randomSeeder.get(), randomSeeder)) {
      throw new UnsupportedOperationException(
          "ReseedingSplittableRandomAdapter's binding to SimpleRandomSeeder is immutable");
    }
  }

  @Override public boolean usesParallelStreams() {
    return true;
  }

  @Override protected ToStringHelper addSubclassFields(final ToStringHelper original) {
    return original.add("randomSeeder", randomSeeder.get()).add("seedGenerator", seedGenerator);
  }

  @Override protected SplittableRandom getSplittableRandom() {
    final SingleThreadSplittableRandomAdapter adapterForThread
        = (SingleThreadSplittableRandomAdapter) threadLocal.get();
    return adapterForThread.getSplittableRandom();
  }

  @Override public double nextGaussian() {
    return threadLocal.get().nextGaussian();
  }

  @Override protected void debitEntropy(final long bits) {
    // Necessary because our inherited next* methods read straight through to the SplittableRandom.
    ((SingleThreadSplittableRandomAdapter) threadLocal.get()).debitEntropy(bits);
  }

  @Override protected void setSeedInternal(final byte[] seed) {
    this.seed = seed.clone();
  }

  @Override public String toString() {
    return "ReseedingSplittableRandomAdapter using " + randomSeeder;
  }

  @Override public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SplittableRandomAdapter that = (SplittableRandomAdapter) o;
    return Objects.equals(randomSeeder.get(), that.randomSeeder.get())
        && Objects.equals(seedGenerator, that.seedGenerator);
  }

  @Override public int hashCode() {
    return Objects.hash(randomSeeder.get(), seedGenerator);
  }
}
