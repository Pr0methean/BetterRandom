package io.github.pr0methean.betterrandom.prng;

import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.pr0methean.betterrandom.seed.RandomSeederThread;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SeedGenerator;
import io.github.pr0methean.betterrandom.util.BinaryUtils;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Random;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * Wraps a {@link ThreadLocal}&lt;{@link BaseRandom}&gt; in order to provide concurrency that most
 * implementations of {@link BaseRandom} can't implement naturally.
 */
public class ThreadLocalRandomWrapper extends BaseRandom {

  private static final long serialVersionUID = 1199235201518562359L;
  private transient ThreadLocal<BaseRandom> threadLocal;
  private final Supplier<BaseRandom> initializer;
  private final @Nullable
  Integer explicitSeedSize;

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    threadLocal = ThreadLocal.withInitial(initializer);
  }

  /**
   * Uses this class and {@link RandomWrapper}, this can be used to decorate any implementation of
   * {@link Random} that can be constructed from a {@code long} seed into a fully-concurrent one.
   *
   * @param legacyCreator a function that provides the {@link Random} that underlies the returned
   *     wrapper on each thread.
   * @return a ThreadLocalRandomWrapper decorating instances created by {@code legacyCreator}.
   * @throws SeedException should never happen.
   */
  public static ThreadLocalRandomWrapper wrapLegacy(
      LongFunction<Random> legacyCreator, SeedGenerator seedGenerator) throws SeedException {
    return new ThreadLocalRandomWrapper(Long.BYTES, seedGenerator, bytes ->
        new RandomWrapper(legacyCreator.apply(BinaryUtils.convertBytesToLong(bytes))));
  }

  /**
   * Wraps the given {@link Supplier}. This ThreadLocalRandomWrapper will be serializable if the
   * {@link Supplier} is serializable.
   *
   * @param initializer a supplier that will be called to provide the initial {@link BaseRandom} for
   * each thread.
   * @throws SeedException should never happen.
   */
  public ThreadLocalRandomWrapper(Supplier<BaseRandom> initializer) throws SeedException {
    super(0);
    this.initializer = initializer;
    threadLocal = ThreadLocal.withInitial(initializer);
    explicitSeedSize = null;
  }

  /**
   * Wraps a seed generator and a function that takes a seed byte array as input.
   *
   * @param seedSize the size of seed arrays to generate.
   * @param seedGenerator The seed generation strategy that will provide the seed value for each
   *     thread's {@link BaseRandom}.
   * @param creator a {@link Function} that creates a {@link BaseRandom} from each seed. Probably a
   *     constructor reference.
   * @throws SeedException should never happen.
   */
  public ThreadLocalRandomWrapper(int seedSize, SeedGenerator seedGenerator,
      Function<byte[], BaseRandom> creator) throws SeedException {
    super(0);
    explicitSeedSize = seedSize;
    initializer = (Serializable & Supplier<BaseRandom>) (() -> {
      try {
        return creator.apply(seedGenerator.generateSeed(seedSize));
      } catch (SeedException e) {
        throw new RuntimeException(e);
      }
    });
    threadLocal = ThreadLocal.withInitial(initializer);
  }

  /**
   * Not supported, because this class uses a thread-local seed.
   *
   * @param thread ignored.
   * @throws UnsupportedOperationException always.
   */
  @Override
  public void setSeederThread(@Nullable RandomSeederThread thread) {
    throw new UnsupportedOperationException("This can't be reseeded by a RandomSeederThread");
  }

  @Override
  protected boolean useParallelStreams() {
    return true;
  }

  @Override
  protected int next(int bits) {
    return threadLocal.get().next(bits);
  }

  @Override
  protected ToStringHelper addSubclassFields(ToStringHelper original) {
    return original.add("threadLocal", threadLocal);
  }

  @Override
  public byte[] getSeed() {
    return threadLocal.get().getSeed();
  }

  @Override
  protected void setSeedInternal(byte[] seed) {
    threadLocal.get().setSeed(seed);
  }

  @Override
  public long getEntropyBits() {
    return threadLocal.get().getEntropyBits();
  }

  @Override
  public int getNewSeedLength() {
    return (explicitSeedSize == null) ? threadLocal.get().getNewSeedLength() : explicitSeedSize;
  }
}
